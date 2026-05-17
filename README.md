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
Conforme os requisitos do projeto final, a suíte de testes cumpre a política **"No-Mocks"**, o que significa que o uso do `Mockito` está vedado. As estratégias adotadas são:

- **Teste de Caixa Preta (E2E):** O `BookControllerE2ETest` levanta o contexto web real (`@SpringBootTest` e `MockMvc` integrado) validando as rotas da ponta da API até a persistência no banco real.
- **Teste de Caixa Branca & Parametrizados:** O `UserServiceTest` valida fluxos condicionais (usuários repetidos) e possui fluxos iterativos (`@ParameterizedTest`).
- **Testcontainers:** Todo o armazenamento durante os testes ocorre via uma imagem Docker do MongoDB sendo manipulada pelo código sob os panos no `AbstractIntegrationTest`.
- **VCR (Video Cassette Recorder):** O `BookServiceTest` simula chamadas externas do `OpenLibraryClient` usando o **WireMock**, interceptando as requisições HTTP e devolvendo os JSONs reais armazenados na regra de teste.

## Cobertura de Teste
O projeto conta com validação de build pelo **JaCoCo**. Para a pipeline do CI passar e o build gerar com sucesso, **pelo menos 80% das linhas do projeto devem estar cobertas**.

Para gerar o relatório de cobertura, execute:
```bash
mvn clean verify
```
O relatório HTML estará disponível em `target/site/jacoco/index.html`.

## Como Rodar Localmente
1. Tenha o **Docker** rodando na sua máquina (caso vá rodar os testes).
2. Tenha um **MongoDB** rodando localmente na porta `27017` ou adapte o `application.properties`.
3. Inicie o projeto Spring Boot:
```bash
mvn spring-boot:run
```
4. Acesse pelo navegador: `http://localhost:8080`
