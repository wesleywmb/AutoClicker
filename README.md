# AutoClicker Pro

Aplicativo desktop de automação de cliques para Windows, escrito em Java 11 com Swing.

## Recursos

- Intervalo de 0,001 a 60 segundos, interpretado como uma pausa após cada ação de clique.
- Botões esquerdo, direito e central, com clique simples ou duplo.
- Execução contínua ou limitada a 999.999 ações.
- Contagem regressiva antes de iniciar e parada responsiva pela interface ou atalho global.
- Posição fixa compatível com coordenadas negativas de monitores secundários.
- Captura de posição em três segundos.
- Perfis persistentes para as configurações de clique.
- Macros em memória com ações de clique, movimento e espera.
- Métricas de tempo e ações/ciclos por segundo.

Uma ação dupla conta como uma ação. No modo macro, o limite e as métricas representam ciclos completos. As sequências de macro não são salvas nos perfis nesta versão.

## Requisitos

- Windows 10 ou 11.
- JDK 11 ou superior. É necessário o JDK completo, pois o projeto usa `javac` para compilar.
- Acesso à internet na primeira compilação, caso as bibliotecas da pasta `lib` ainda não estejam disponíveis.
- Maven é opcional e só é necessário para executar os testes ou fazer o build Maven.

## Como executar — passo a passo

### Opção 1: script automático (recomendado)

1. Instale um JDK 11 ou superior, como Eclipse Temurin, Microsoft OpenJDK ou Oracle JDK.
2. Durante a instalação, habilite a opção de adicionar o Java ao `PATH`, quando ela estiver disponível.
3. Feche e abra novamente o terminal ou o Visual Studio Code.
4. Abra o Prompt de Comando ou o terminal do VS Code e confirme a instalação:

   ```bat
   java -version
   javac -version
   ```

   Os dois comandos devem mostrar a versão 11 ou superior.

5. Entre na pasta raiz do projeto. No terminal integrado do VS Code, aberto neste projeto, você já deve estar nela. Caso contrário:

   ```bat
   cd C:\caminho\para\AutoClicker-Pro
   ```

6. Execute:

   ```bat
   tools\iniciar.bat
   ```

7. Na primeira execução, aguarde o download das dependências e a compilação. A janela do AutoClicker Pro será aberta automaticamente.

O `iniciar.bat` recompila o projeto quando alguma fonte Java é mais recente que o JAR. Os scripts conferem o SHA-256 das bibliotecas antes de compilar ou executar. Também é possível abrir a pasta `tools` pelo Explorador de Arquivos e dar dois cliques em `iniciar.bat`.

### Opção 2: compilar e executar separadamente

Na raiz do projeto, compile com:

```bat
tools\compilar.bat
```

Depois execute o aplicativo com:

```bat
java -cp "build\autoclicker-pro.jar;lib\jnativehook-2.2.2.jar;lib\flatlaf-3.2.5.jar" com.autoclicker.ui.AutoClickerUI
```

Em Java 24 ou superior, acrescente `--enable-native-access=ALL-UNNAMED` logo após `java` na execução manual. O `iniciar.bat` detecta essas versões e aplica a opção automaticamente.

### Opção 3: Maven

Com Java e Maven disponíveis no `PATH`:

```bat
mvn clean test
mvn package
java -jar target\autoclicker-pro.jar
```

O artefato criado pelo Maven contém as dependências. O JAR da pasta `build`, criado pelo script, deve ser executado com as bibliotecas da pasta `lib`, como mostrado na opção 2.

### Criar um atalho na Área de Trabalho

Depois de confirmar que o aplicativo abre corretamente, execute:

```bat
tools\criar-atalho.bat
```

O script cria `AutoClicker-Pro.bat` na Área de Trabalho. Depois disso, basta dar dois cliques nesse arquivo para iniciar o programa.

## Como usar

1. Em **Intervalo**, informe a pausa depois de cada ação de clique.
2. Em **Mouse**, escolha o botão e o tipo de clique.
3. Para clicar sempre no mesmo lugar, marque **Usar posição fixa** e clique em **Capturar em 3s**. Posicione o cursor no alvo antes do fim da contagem.
4. Escolha a execução contínua ou informe o limite de ações. Se desejar, configure uma contagem antes do início.
5. Clique em **Iniciar** ou pressione F6. O atalho funciona mesmo com a janela minimizada.
6. Para parar, clique em **Parar** ou pressione F6 novamente.

Para criar uma macro, marque **Usar sequência de ações** e adicione cliques, movimentos e esperas na ordem desejada. No modo macro, o limite representa ciclos completos da sequência.

Os perfis salvam as configurações de clique para uso posterior. As ações da macro permanecem apenas em memória e são apagadas quando o aplicativo é fechado.

O status e o botão de controle permanecem visíveis no rodapé. Controles que alteram a sessão ficam bloqueados enquanto ela está ativa.

## Testes

Os testes usam implementações simuladas de mouse, relógio e espera; não geram cliques reais:

```bash
mvn test
```

Além dos testes automatizados, alterações de interface devem ser verificadas no Windows com a janela minimizada, múltiplos monitores, escalas diferentes e fechamento durante uma execução.

## Dependências

- JNativeHook 2.2.2 para teclado global.
- FlatLaf 3.2.5 para o tema Swing.
- JUnit Jupiter 5.10.2 para testes.
