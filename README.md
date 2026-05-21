# Gerenciador de Biblioteca Pessoal

Um projeto de aplicação Web MVC com Spring Boot e MongoDB, focado nas melhores práticas de teste de software automatizado.

## Tecnologias Utilizadas
- **Java 17**
- **Spring Boot 3.2.x** (Web, Data MongoDB, Security, Validation)
- **Thymeleaf** com Extras do Spring Security
- **MongoDB** (Persistência)
- **CSS Puro** (Design Amarelo e Verde com Glassmorphism)
- **Testcontainers** (Ambiente de banco de dados real em testes)
- **WireMock** (Para VCR - Gravação/Reprodução de APIs Externas)
- **GitHub Actions** (CI/CD)
- **SonarQube/SonarCloud** (Análise de Qualidade)
- **JaCoCo** (Cobertura de Testes)

## Funcionalidades
1. Cadastro de novos usuários.
2. Autenticação e gerenciamento seguro de sessão.
3. Dashboard responsiva e dinâmica.
4. CRUD de livros lidos ou pendentes na biblioteca pessoal.
5. Busca automática de metadados do livro por ISBN através da OpenLibrary API.

## Estratégia de Testes Aplicada
Conforme os requisitos do projeto, a suíte de testes cumpre a política **"No-Mocks"**, o que significa que não há uso de `@MockBean`, `@Mock`, `Mockito.mock()` ou qualquer atalho sintético de autenticação (`SecurityMockMvcRequestPostProcessors.user()`). As estratégias adotadas são:

- **Teste de Caixa Preta (E2E):** Três classes cobrem os controllers de ponta a ponta usando `@SpringBootTest`, com autenticação feita via `POST /login` real — o `UsernamePasswordAuthenticationFilter` do Spring Security é exercitado, o `CustomUserDetailsService` consulta o MongoDB real (Testcontainers) e o BCrypt verifica a senha. A sessão autenticada (`MockHttpSession`) é capturada e reutilizada nos testes seguintes:
  - `AuthControllerE2ETest` — cobre login com sucesso, login com credenciais inválidas, cadastro, validações e erro de username duplicado.
  - `BookControllerE2ETest` — cobre listagem, adição e restrição de acesso para não autenticados.
  - `BookControllerExtendedE2ETest` — cobre edição, deleção, validação de formulário e busca por ISBN via VCR declarativo.

- **Teste de Caixa Branca & Parametrizados:** O `UserServiceTest` valida fluxos condicionais internos (usuário duplicado, hash de senha) e possui testes iterativos com `@ParameterizedTest`.

- **Testcontainers:** Todo o armazenamento durante os testes ocorre via uma imagem Docker do MongoDB gerenciada pelo `AbstractIntegrationTest`, compartilhada por todas as classes de teste.

- **VCR (Video Cassette Recorder):** O WireMock é utilizado em dois modos:
  - **Programático** — `BookServiceTest` define os stubs diretamente no código com `stubFor`.
  - **Declarativo** — `BookControllerExtendedE2ETest` carrega cassetes gravados em `src/test/resources/mappings/` e `src/test/resources/__files/`, simulando respostas reais da OpenLibrary API sem acesso à internet.

## Cobertura de Teste
O projeto conta com validação de build pelo **JaCoCo**. Para a pipeline do CI passar e o build ser gerado com sucesso, **pelo menos 80% das linhas do projeto devem estar cobertas**.

Para gerar o relatório de cobertura, execute:
```bash
mvn clean verify
```
O relatório HTML estará disponível em `target/site/jacoco/index.html`.

## Como Rodar Localmente
1. Tenha o **Docker** rodando na sua máquina (necessário para os Testcontainers).
2. Tenha um **MongoDB** rodando localmente na porta `27017` ou adapte o `application.properties`.
3. Inicie o projeto Spring Boot:
```bash
mvn spring-boot:run
```
4. Acesse pelo navegador: `http://localhost:8080`