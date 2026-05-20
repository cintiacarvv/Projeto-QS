# Matriz de Rastreabilidade de Requisitos (RTM)

## Tabela de Rastreabilidade

| ID Req | Descrição do Requisito Funcional | Tipo de Teste | Classe(s) de Teste | Ferramentas | Status |
|---|---|---|---|---|---|
| **RF01** | O sistema deve permitir o cadastro de novos usuários no banco MongoDB. | Integração Parametrizado, Caixa Branca, E2E | `UserServiceTest`, `AuthControllerE2ETest` | JUnit, Testcontainers, MockMvc | Concluído |
| **RF02** | O sistema deve garantir que o nome de usuário seja único. | Integração, E2E | `UserServiceTest`, `AuthControllerE2ETest` | JUnit, Testcontainers, MockMvc | Concluído |
| **RF03** | O sistema deve permitir login de usuários cadastrados gerenciando a sessão. | E2E (Controller) | `AuthControllerE2ETest` | MockMvc, Testcontainers | Concluído |
| **RF04** | O sistema deve impedir o acesso a rotas privadas para usuários não autenticados. | E2E (Controller) | `BookControllerE2ETest` | MockMvc, Testcontainers | Concluído |
| **RF05** | O sistema deve realizar operações de CRUD (Criar, Ler, Atualizar, Deletar) para Livros. | E2E (Controller) | `BookControllerE2ETest`, `BookControllerExtendedE2ETest` | MockMvc, Testcontainers | Concluído |
| **RF06** | O sistema deve permitir a busca de um livro por ISBN utilizando API externa. | Integração (VCR), E2E (VCR) | `BookServiceTest`, `BookControllerExtendedE2ETest` | JUnit, WireMock | Concluído |

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

### RF03 e RF04: Autenticação e Restrição de Acesso
```mermaid
sequenceDiagram
    actor U as Usuário
    participant F as SecurityFilter
    participant C as BookController

    U->>F: GET /books (não autenticado)
    F-->>U: Redirect to /login

    U->>F: POST /login (credenciais)
    F->>F: Authenticate & Create Session
    F-->>U: Redirect to /books

    U->>F: GET /books (com sessão)
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
```