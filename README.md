# aglerr-bot
System ordered by aglerr (299801068450086912) via DevRoom. - Amount paid: $27

[Download the programs](https://github.com/DarkPizza/aglerr-bot#download-the-required-programs)

[How do I turn on the bot?](https://github.com/DarkPizza/aglerr-bot#how-do-i-turn-on-the-bot)

[How do I turn off the bot?](https://github.com/DarkPizza/aglerr-bot#how-to-i-turn-off-the-bot)

[How do I compile the bot .jar?](https://github.com/DarkPizza/aglerr-bot#how-do-i-compile-the-bot-jar)

![image](https://user-images.githubusercontent.com/41030800/219953352-d5aeb18e-9752-4eff-acda-f7856e29d7a5.png)

.

.

.

.

.

# Download the required programs
> Edit the codes and messages: IntelliJ IDEA Community Edition (https://www.jetbrains.com/idea/download/)

# How do I turn on the bot?
> This bot was made exclusively in Java 100% and can only run on a machine that has it installed. then you just need to put the compiled .jar in your `/bot/` folder
> You will also need to create the bot configuration files, to do this simply follow the steps below. 

1. Create a folder called `/resources/` in your server's home directory.
2. Send all the `.properties` files into that folder, just them and nothing else. 
3. Now you need to edit all the `.properties` files with the settings you want, as arranged within them. 

4. With all the settings done, you will need to grab your bot's `.jar` and drop it into the `/bot/` folder, just like a normal file. 

5. Go to your terminal/cmd and use the command `cd /bot/` and the `java -jar --add-opens java.base/java.lang=ALL-UNNAMED bot.jar`

**Don't forget this flag, otherwise you'll get errors when you start the bot!**


# How to I turn off the bot?
1. Just use the CTRL + C in your command line while the bot in online.

# How do I compile the bot .jar?
> For you to compile the .jar of your bot so that it is ready to be placed in your folder with the modifications you made, is super simple.

1. Locate the Gradle tab, normally located on the right side of your screen vertically.
![image](https://github.com/DarkPizza/aglerr-bot/assets/41030800/d2306f9c-018d-4b3d-9983-14ba1593665e)

2. Open the folders with arrows, which are `aglerr-bot > Tasks > shadow`. Then double-click the `shadowJar` option
![image](https://github.com/DarkPizza/aglerr-bot/assets/41030800/e8f121ea-10c1-4e71-b325-69ed74369aaf)

3. It will then open a new console-like menu that will show that shadowJar is running and compiling its .jar.
![image](https://github.com/DarkPizza/aglerr-bot/assets/41030800/b0046301-5395-4b1b-a0c8-4f0cb2b664d1)

4. When your .jar has been compiled, you will be shown a message like 04:56:59: Execution finished 'shadowJar'.
5. And then, your compiled .jar will be located inside the `build > libs` folder.
![image](https://github.com/DarkPizza/aglerr-bot/assets/41030800/ebe5f28d-7c0c-4fed-8c5f-c52d846e53f7)
