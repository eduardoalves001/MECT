
# TaskMaster
## Sistema de Recompensas e Progressão baseado em Tarefas
Projeto de colaboração entre estudantes do DETI para âmbito da disciplina EGS E DECA para âmbito do projeto Milho a Pardais.

## Participantes
### Estudantes
| NMec | Nome | Email |
|:---:|:---|:---:|
| 104179 | EDUARDO ALVES | eduardoalves@ua.pt |
| 98466 | RAFAEL SANTOS | rafaelmsantos@ua.pt |
| 103709 | RUI CAMPOS | ruigabriel2@ua.pt |
| 103270 | GABRIEL COUTO | gabrielcouto@ua.pt |
| 125714 | INÊS SOARES | ines.azevedo.soares@ua.pt |
| 119895 | VANESSA MELO | vanessa.magalhaes@ua.pt |
| 124544 | PAUL MARTINS | paul.martins@ua.pt |

### Supervisores
| Nome | Email |
|:---|:---:|
| DIOGO GOMES| dgomes@ua.pt |
| CARLOS SANTOS| carlossantos@ua.pt |

## Conceito
Implementação de um Composer (FrontEnd), que comunica com diferentes micro-serviços usando APIs. Dentro destes micro-serviços planeamos implementar um sistema de Autenticação que, se completo, permite uma autenticação física usando NFC e uma autenticação digital usando o IDP da UA. Planeamos também desenvolver um micro-serviço relacionado com o uso de notificações e um relacionado com um sistema de pontos. Este sistema de pontos, permitirá, no nosso caso em particular, calcular um bónus de nota atribuível a estudantes que participem nas aulas, ou sejam ativos nos projetos de grupo através de contribuições no GitHub.

## Autenticação
For authentication to work with python/flask:

Cria um ambiente virtual na pasta do projeto:
python3 -m venv venv
source venv/bin/activate
pip install flask authlib

Finally:
python3 auth.py

## Composer
run npm install to install omdules and dependencies
