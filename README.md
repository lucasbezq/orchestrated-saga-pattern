# WIP: Microsserviços: Implementação do Padrão Saga Orquestrado

![Arquitetura Proposta](https://github.com/user-attachments/assets/e019b478-1edc-4a1d-bd85-827f31e2abdc)

## 🎯 Objetivo do Projeto
Este projeto demonstra a implementação de uma arquitetura de microsserviços resiliente utilizando o **Padrão Saga com Orquestração**. O foco principal é resolver o desafio da **consistência eventual** em transações distribuídas, garantindo que operações complexas de negócio (como a criação de um pedido) sejam concluídas com sucesso ou devidamente revertidas em caso de falha.

---

## 🛠 Stack Tecnológica

| Tecnologia | Descrição |
| :--- | :--- |
| **Java 17** | Linguagem principal utilizando recursos modernos da JVM. |
| **Spring Boot 3** | Framework base para os microsserviços. |
| **Apache Kafka** | Backbone de mensageria para comunicação assíncrona. |
| **PostgreSQL** | Banco relacional para serviços com transações ACID locais. |
| **MongoDB** | Banco NoSQL para persistência flexível de pedidos. |
| **Docker / Compose** | Conteinerização e orquestração de infraestrutura local. |
| **Redpanda Console** | Interface visual para gestão e monitoramento do Kafka. |
| **Gradle** | Gestão de dependências e automação de build. |

---

## 🏗 Arquitetura e Componentes

A solução é composta por **5 microsserviços** que interagem de forma coordenada:

### 1. 🛒 Order-Service
* **Papel:** Entrypoint do sistema.
* **Função:** Recebe o pedido via API REST, persiste o estado inicial no **MongoDB** e inicia a Saga notificando o Orquestrador.
* **Responsabilidade:** Expor o status final do processamento para o cliente.

### 2. 🎼 Orchestrator-Service
* **Papel:** O "Maestro" do sistema.
* **Função:** Detém a máquina de estados da transação. Ele decide qual serviço deve ser chamado em seguida e gerencia a lógica de **compensação (rollback)** caso ocorra erro em qualquer etapa.
* **Nota:** Não possui banco de dados próprio, mantendo o estado na memória da transação distribuída via Kafka.

### 3. 🔍 Product-Validation-Service
* **Papel:** Validação de Domínio.
* **Função:** Verifica a integridade e existência dos produtos solicitados.
* **Persistência:** PostgreSQL.

### 4. 💳 Payment-Service
* **Papel:** Financeiro.
* **Função:** Processa o pagamento com base nos valores e quantidades. É um ponto crítico onde falhas (ex: falta de saldo) disparam rollbacks no estoque e validação.
* **Persistência:** PostgreSQL.

### 5. 📦 Inventory-Service
* **Papel:** Logística.
* **Função:** Realiza a reserva e baixa técnica de estoque.
* **Persistência:** PostgreSQL.

---

## 🔄 Fluxo da Saga Orquestrada

1.  **Sucesso:** Cada serviço processa sua parte e responde `Success` ao Orquestrador, que avança até o `Notify End Saga`.
2.  **Falha (Rollback):** Se o `Payment-Service` falhar, o Orquestrador consome esse evento e envia um comando de compensação para o `Product-Validation` e `Inventory` para desfazer as reservas, garantindo a integridade dos dados.

---
