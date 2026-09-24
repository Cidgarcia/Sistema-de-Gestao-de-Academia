# Desempenho — GymCore

## Como reproduzir

Execute `mvn -Dtest=DesempenhoTest test`. O teste cria um SQLite temporário com 100 alunos, 100 matrículas e 1.000 frequências. Ele não altera o banco de uso diário. Os tempos abaixo foram observados em 24/09/2026 neste computador; podem variar conforme máquina, cache e carga do sistema.

## Medições com a base ampliada

| Operação | Tempo observado |
|---|---:|
| Cadastro de aluno | 7 ms |
| Atualização de aluno | 6 ms |
| Busca de aluno por nome | 4 ms |
| Listagem de alunos e matrículas | 18 ms |
| Registro de entrada por CPF | 21 ms |
| Histórico completo (1.001 entradas, incluindo a entrada do teste) | 40 ms |
| Histórico recente (10 entradas) | 9 ms |
| Sincronização e listagem de pendências | 25 ms |
| Carregamento do FXML de login | 559 ms |
| Carregamento do painel de Funcionário | 762 ms |
| Carregamento do painel de Instrutor | 309 ms |

O teste verifica os limites de 500 ms para cadastro/atualização, 2 s para carregar cada tela e os limites específicos das consultas existentes. O carregamento do FXML inclui a inicialização do controlador, mas **não** mede a pintura da janela na tela; uma avaliação visual de fluidez ainda precisa ser feita no aplicativo.

## Consulta lenta e comparação antes/depois

A busca por CPF normalizado em `Alunos` usa `replace(replace(cpf, '.', ''), '-', '')`. Sem índice de expressão, o plano de consulta apresenta `SCAN Alunos`; com `idx_alunos_cpf_limpo`, apresenta `SEARCH Alunos USING INDEX idx_alunos_cpf_limpo`. Para comparação reproduzível, o teste executa a mesma consulta com `NOT INDEXED` (simulação do cenário sem índice) e normalmente, na mesma base e no mesmo processo:

| 1.000 buscas por CPF | Sem índice | Com índice |
|---|---:|---:|
| Tempo observado | 49 ms | 12 ms |

Esse é um comparativo controlado da consulta, **não** uma medição histórica de versões anteriores do aplicativo. Os índices de CPF, matrículas, frequência e pendências já constam de `schema.sql` e das migrações em `ConexaoSQLite.java`, inclusive para bancos existentes. Nenhum índice novo foi necessário nesta revisão.

## Carregamentos na interface

A abertura da tela de frequência carrega apenas as dez entradas recentes. A consulta de histórico sem filtros limita a resposta a 250 registros; o teste confirma esse limite com 1.000 entradas. O histórico completo continua acessível por consulta explícita. Não há evidência aqui de paginação geral ou de *lazy loading* em todas as telas; essa alegação foi removida.

## Checklist

- [x] Massa de 100 alunos e 1.000 frequências.
- [x] Cadastro, busca, atualização e carregamento das telas medidos.
- [x] Plano de consulta identificou varredura de CPF sem o índice.
- [x] Índices necessários conferidos no SQLite e nas migrações.
- [x] Carga automática de frequência limitada e testada.
- [x] Resultados sem/com índice documentados com metodologia reproduzível.
- [x] Tempos automatizados dentro dos limites acima com a base ampliada.

Para aceitar a experiência visual como concluída, ainda é recomendável abrir o aplicativo e observar a navegação nas telas com os dois perfis; os testes automatizados não substituem essa checagem.
