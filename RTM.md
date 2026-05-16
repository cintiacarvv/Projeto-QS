# Matriz de Rastreabilidade de Requisitos (RTM)

## Tabela de Rastreabilidade

| ID Req | Descrição do Requisito Funcional | Tipo de Teste (Local) | Ferramentas | Status |
|---|---|---|---|---|
| **RF01** | O sistema deve permitir o cadastro de novos usuários no banco MongoDB. | Teste de Integração Parametrizado, Caixa Branca | JUnit, Testcontainers | Concluído |
| **RF02** | O sistema deve garantir que o nome de usuário seja único. | Teste de Integração | JUnit, Testcontainers | Concluído |
| **RF03** | O sistema deve permitir login de usuários cadastrados gerenciando a sessão. | Teste E2E (Controller) | MockMvc, Testcontainers | Concluído |
| **RF04** | O sistema deve impedir o acesso a rotas privadas para usuários não autenticados. | Teste E2E (Controller) | MockMvc, Testcontainers | Concluído |
| **RF05** | O sistema deve realizar operações de CRUD (Criar, Ler, Atualizar, Deletar) para Livros. | Teste E2E (Controller) / Integração | MockMvc, Testcontainers | Concluído |
| **RF06** | O sistema deve permitir a busca de um livro por ISBN utilizando API externa. | Teste de Integração (VCR) | JUnit, WireMock | Concluído |

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

### RF03 e RF04: Autenticação e Restrição
```mermaid
sequenceDiagram
    actor U as Usuário
    participant F as SecurityFilter
    participant C as BookController

    U->>F: GET /books (Not Logged in)
    F-->>U: Redirect to /login
    
    U->>F: POST /login (credentials)
    F->>F: Authenticate & Create Session
    F-->>U: Redirect to /books
    
    U->>F: GET /books (With Session)
    F->>C: forward request
    C-->>U: return books/list.html
```

### RF05: Adicionar Livro (CRUD)
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
```

### RF06: Buscar Livro por ISBN (API Externa com VCR)
```mermaid
sequenceDiagram
    actor U as Usuário
    participant C as BookController
    participant S as BookService
    participant API as OpenLibraryClient
    participant W as OpenLibrary API (WireMock nos Testes)

    U->>C: GET /books/add?isbn=0451526538
    C->>S: findByIsbnExternal("0451526538")
    S->>API: findBookByIsbn("0451526538")
    API->>W: GET /api/books?bibkeys=ISBN:...
    W-->>API: JSON Response (Mocked in VCR)
    API->>API: Parse JSON to Book Object
    API-->>S: Book Object
    S-->>C: Book Object
    C-->>U: Render books/form.html with prepopulated data
```
