# Nome do Projeto

Breve descrição do que este projeto faz.

## Setup

Esta seção descreve os passos necessários para configurar e rodar o projeto localmente.

### Backend

Para configurar o ambiente de backend, você precisará definir as seguintes variáveis de ambiente. Recomenda-se criar um arquivo `application.properties` na pasta `src/main/resources` e preencher com os seguintes valores:

#### URL do frontend
- **`FRONTEND_URL`**:
  - *Descrição*: URL base de onde está rodando seu aplicativo frontend. Usado para permitir requisições (CORS).
  - *Exemplo*: http://localhost:8080/

#### Database
- **`SPRING_DATASOURCE_URL`**:
  - *Descrição*: Credenciais de acesso ao seu banco de dados PostgreSQL.
  - *Preencher com*: Crie uma conta no site https://neon.com/ e crie um projeto lá, clique em Connect e selecione a Opção Java de connection String, copie ela e cole aqui.


#### Pipefy
- **`CONFIGURAÇÃO DO PIPE`**:
    Você vai criar um Pipe na Opção **Criar Pipe do Zero** com o Nome de **"Pŕe-Vendas"** e uma Fase dentro dele com o nome de **"Pré-Vendas"**, após isso você deve clicar em **+ Criar Novo Card** e criar um Card com os campos a seguir com o **MESMO NOME E TIPO DE CAMPO**:
![NomeField](tutorial/NomeField.png)
![EmailField](tutorial/EmailField.png)
![EmpresaField](tutorial/EmpresaField.png)
![NecessidadeField](tutorial/NecessidadeField.png)
![InteressadoField](tutorial/InteressadoField.png)
![MeetingLink](tutorial/MeetingLink.png)
![MeetingDate](tutorial/MeetingDate.png)

### É importante na tela de Criação do Card você clicar  em **Configurações do Pipe** e escolher o Campo Título do Card como E-mail! Seu Card no final deve parecer com isso:

![MeetingDate](tutorial/PipefyCard.png)


- **`PIPEFY_TOKEN`**:
  - *Descrição*: Token de API e ID do Pipe no Pipefy para integração.
  - *Preencher com*: Crie uma Conta no Pipefy e gere seu token aqui https://app.pipefy.com/tokens.

- **`PIPEFY_PIPE_ID`**:
  - *Preencher com*: É o ID do Pipe de Pré-Vendas que você criou, você consegue pegar ele na URL do Pipe que você criou ele é esse número aqui.

    ![PIPEFY_PIPE_ID](tutorial/pipeID.png)


#### Calendly
- **`CALENDLY_TOKEN`**:
  - *Descrição*: Token de API do Calendly.
  - *Preencher com*: Você vai precisar criar uma conta no Calendly, conectar ela com o Google Calendar (vai ter essa opção enquanto você estiver criando a conta), o tempo no Scheduling eu deixei assim (8:00 as 17:00):

    ![Scheduling](tutorial/Scheduling.png)

    Depois basta você ir em **Integrations & Apps** https://calendly.com/integrations/api_webhooks e criar seu Token e usar ele aqui.

- **`CALENDLY_CALLBACK`**:
  - *Descrição*: URL de callback do seu backend para receber webhooks.
  - *Preencher com*: Para usar localmente você precisará instalar o **NGROK** https://ngrok.com/download/windows, criar uma conta e pegar seu **AuthToken** no site deles, abrir o terminal e rodar

    ```sh
    ngrok config add-authtoken $SEU_AUTHTOKEN
    ngrok http $PORTA_QUE_QUER_USAR
    ```
    ele vai te devolver algo assim:
    ```sh
     https://jeromy-uncheating-unornately.ngrok-free.dev
    ```
    ai basta colocar no **CALENDLY_CALLBACK**:
    ```sh
     https://jeromy-uncheating-unornately.ngrok-free.dev/calendly/webhook
    ```



#### OpenAI
- **`OPENAI_TOKEN`**:
  - *Descrição*: Chave de API da OpenAI para acessar os modelos de linguagem.
  - *Preencher com*: Basta ter uma conta na OpenAI e criar uma chave e usar ela aqui.

### Frontend

*(Instruções de configuração do frontend a serem adicionadas aqui.)*