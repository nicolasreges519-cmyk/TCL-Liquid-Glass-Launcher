# TCL Liquid Glass Launcher — versão de desenvolvimento

Projeto de launcher para TCL P7L com Google TV / Android 12.

## Estado atual
- Menu navegável por controle remoto (setas/OK)
- Listagem de apps com ícones reais e abertura por toque/OK
- Alternância de aparência translúcida e ajuste do tamanho das linhas
- Acesso a configurações do sistema e seletor de papel de parede (quando suportado)
- Workflow GitHub Actions para compilar APK de teste

## Limitações conhecidas
- A aparência ainda não reproduz integralmente o mockup Liquid Glass aprovado.
- Cartazes de filmes, recomendações dinâmicas, clima e widgets personalizáveis **não estão implementados**.
- A compilação e o comportamento real na TCL P7L precisam ser verificados.
- O sistema Google TV pode impedir que um launcher de terceiros substitua a tela inicial padrão; não desative o launcher original.

## Testar sem substituir a tela inicial
1. Abra a aba **Actions** no GitHub e selecione **Verificar APK Liquid Glass**.
2. Se a execução concluir com sucesso, baixe o artefato **TCL-Liquid-Glass-APK-teste**.
3. Extraia o ZIP para obter o APK.
4. Instale somente por um método confiável e autorizado na TV e inicie como aplicativo normal.
5. Teste setas, OK, Voltar, lista de apps e configurações.

## Segurança
Não exige root. Não remova o launcher original da TV. A ramificação `melhorias-launcher-liquid-glass` mantém as mudanças isoladas até a revisão.
