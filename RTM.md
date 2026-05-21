# Matriz de Rastreabilidade de Requisitos (RTM)

## Tabela de Rastreabilidade

| ID Req | Descrição do Requisito Funcional | Tipo de Teste | Classe(s) de Teste | Métodos de Teste | Ferramentas | Status |
|---|---|---|---|---|---|---|
| **RF01** | O sistema deve permitir o cadastro de novos usuários no banco MongoDB. | Integração Parametrizado, Caixa Branca, E2E | `UserServiceTest`, `AuthControllerE2ETest` | `registerUser_ShouldHashPasswordAndSave`, `registerUser_MultipleUsers_ShouldSaveAll`, `processRegistration_ShouldRedirect_WhenValidData`, `processRegistration_ShouldReturnRegister_WhenPasswordTooShort`, `processRegistration_ShouldReturnRegister_WhenFieldsAreBlank` | JUnit, Testcontainers, WireMock | Concluído |
| **RF02** | O sistema deve garantir que o nome de usuário seja único. | Integração, E2E | `UserServiceTest`, `AuthControllerE2ETest` | `registerUser_ShouldThrowExceptionIfUsernameExists`, `processRegistration_ShouldShowError_WhenUsernameAlreadyExists` | JUnit, Testcontainers | Concluído |
| **RF03** | O sistema deve permitir login de usuários cadastrados gerenciando a sessão. | E2E (Controller), Caixa Preta | `AuthControllerE2ETest` | `login_ShouldRedirectToBooks_WhenValidCredentials`, `login_ShouldRedirectToError_WhenInvalidCredentials` | JUnit, Testcontainers, Spring Security | Concluído |
| **RF04** | O sistema deve impedir o acesso a rotas privadas para usuários não autenticados. | E2E (Controller), Caixa Preta | `BookControllerE2ETest` | `listBooks_ShouldReturnRedirect_WhenNotAuthenticated` | JUnit, Testcontainers, Spring Security | Concluído |
| **RF05** | O sistema deve realizar operações de CRUD (Criar, Ler, Atualizar, Deletar) para Livros. | E2E (Controller), Caixa Preta | `BookControllerE2ETest`, `BookControllerExtendedE2ETest` | `listBooks_ShouldReturnBooks_WhenAuthenticated`, `addBook_ShouldSaveBook_WhenValidFormSubmitted`, `addBook_ShouldReturnForm_WhenValidationFails`, `showEditForm_ShouldReturnFormWithBook`, `updateBook_ShouldSaveAndRedirect_WhenValidData`, `updateBook_ShouldReturnForm_WhenValidationFails`, `deleteBook_ShouldDeleteAndRedirect` | JUnit, Testcontainers | Concluído |
| **RF06** | O sistema deve permitir a busca de um livro por ISBN utilizando API externa. | Integração (VCR), E2E (VCR), Caixa Preta | `BookServiceTest`, `BookControllerExtendedE2ETest` | `findBookByIsbnExternal_ShouldReturnBook_WhenApiReturnsData`, `showAddForm_ShouldReturnEmptyForm_WhenNoIsbn`, `showAddForm_ShouldFillForm_WhenIsbnFoundInApi`, `showAddForm_ShouldShowError_WhenIsbnNotFoundInApi` | JUnit, Testcontainers, WireMock (VCR programático e declarativo) | Concluído |

---

## Estratégia de Autenticação nos Testes

Todos os testes de controller que exigem autenticação utilizam **login real via `POST /login`**, sem qualquer atalho sintético. O fluxo completo é exercitado a cada teste:

1. O `@BeforeEach` faz `POST /login` com credenciais reais
2. O `UsernamePasswordAuthenticationFilter` do Spring Security processa a requisição
3. O `CustomUserDetailsService` consulta o MongoDB real (Testcontainers)
4. O BCrypt verifica a senha
5. A sessão autenticada (`MockHttpSession`) é capturada e reutilizada nos testes

Não há uso de `@MockBean`, `@Mock`, `Mockito.mock()` ou `SecurityMockMvcRequestPostProcessors.user()` em nenhuma classe de teste.

---

## Diagramas UML de Sequência

### RF01 e RF02: Cadastro de Usuário
```mermaid
sequenceDiagram
    actor U as Usuário
    participant C as AuthController
    participant S as UserService
    participant R as UserRepository
    participant DB as MongoDB (Testcontainers)

    U->>C: POST /register (User Form)
    C->>S: registerUser(user)
    S->>R: existsByUsername(username)
    R->>DB: query
    DB-->>R: false
    S->>S: passwordEncoder.encode(password)
    S->>R: save(user)
    R->>DB: insert
    DB-->>R: saved user
    R-->>S: saved user
    S-->>C: success
    C-->>U: Redirect to /login?registered
```

### RF02: Username Duplicado
```mermaid
sequenceDiagram
    actor U as Usuário
    participant C as AuthController
    participant S as UserService
    participant R as UserRepository
    participant DB as MongoDB (Testcontainers)

    U->>C: POST /register (username já existente)
    C->>S: registerUser(user)
    S->>R: existsByUsername(username)
    R->>DB: query
    DB-->>R: true
    S-->>C: throws IllegalArgumentException
    C-->>U: return register.html com errorMessage
```

### RF03: Login e Gerenciamento de Sessão
```mermaid
sequenceDiagram
    actor U as Usuário
    participant F as UsernamePasswordAuthenticationFilter
    participant UDS as CustomUserDetailsService
    participant R as UserRepository
    participant DB as MongoDB (Testcontainers)

    U->>F: POST /login (username, password)
    F->>UDS: loadUserByUsername(username)
    UDS->>R: findByUsername(username)
    R->>DB: query
    DB-->>R: User
    R-->>UDS: User
    UDS-->>F: UserDetails
    F->>F: BCrypt.matches(password, hash)
    F-->>U: Redirect to /books (com sessão autenticada)

    note over F,DB: Em caso de senha errada
    F-->>U: Redirect to /login?error
```

### RF04: Restrição de Acesso a Rotas Privadas
```mermaid
sequenceDiagram
    actor U as Usuário
    participant F as AuthorizationFilter
    participant C as BookController

    U->>F: GET /books (sem sessão)
    F-->>U: Redirect to /login

    U->>F: GET /books (com sessão autenticada)
    F->>C: forward request
    C-->>U: return books/list.html
```

### RF05: CRUD de Livros
```mermaid
sequenceDiagram
    actor U as Usuário
    participant C as BookController
    participant S as BookService
    participant R as BookRepository
    participant DB as MongoDB (Testcontainers)

    U->>C: POST /books/add (Book Form)
    C->>S: save(book)
    S->>R: save(book)
    R->>DB: insert
    DB-->>R: saved book
    R-->>S: saved book
    S-->>C: success
    C-->>U: Redirect to /books

    U->>C: POST /books/edit/{id} (Book Form)
    C->>S: save(book com id)
    S->>R: save(book)
    R->>DB: update
    DB-->>R: updated book
    R-->>S: updated book
    S-->>C: success
    C-->>U: Redirect to /books

    U->>C: GET /books/delete/{id}
    C->>S: deleteById(id)
    S->>R: deleteById(id)
    R->>DB: delete
    DB-->>R: ok
    C-->>U: Redirect to /books
```

### RF06: Buscar Livro por ISBN (API Externa com VCR)
```mermaid
sequenceDiagram
    actor U as Usuário
    participant C as BookController
    participant S as BookService
    participant API as OpenLibraryClient
    participant W as OpenLibrary API (WireMock nos Testes)

    U->>C: GET /books/add?isbn=9780132350884
    C->>S: findByIsbnExternal("9780132350884")
    S->>API: findBookByIsbn("9780132350884")
    API->>W: GET /api/books?bibkeys=ISBN:...
    W-->>API: JSON Response (cassete VCR em /mappings + /__files)
    API->>API: Parse JSON to Book Object
    API-->>S: Book Object
    S-->>C: Book Object
    C-->>U: Render books/form.html com dados preenchidos

    note over API,W: ISBN não encontrado
    W-->>API: HTTP 404
    API-->>S: null
    S-->>C: null
    C-->>U: Render books/form.html com errorMessage
```