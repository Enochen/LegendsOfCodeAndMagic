import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.toList;

class Player {
    public static List<Card> draftHistory = new ArrayList<>();
    public static Boolean amIFirst = null;
    public static long cTime;
    //final static long timeout = 80000000;
    final static long timeout = 100000000;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);

        // game loop
        while (true) {
            String output = "";

            Hero[] heroes = new Hero[2];

            for (int i = 0; i < 2; i++) {
                int playerHealth = in.nextInt();
                int playerMana = in.nextInt();
                int playerDeck = in.nextInt();
                int playerRune = in.nextInt();
                int playerDraw = in.nextInt();

                heroes[i] = new Hero(playerHealth, playerMana, playerDeck, playerRune);
            }
            cTime = System.nanoTime();

            int opponentHand = in.nextInt();

            int opponentActions = in.nextInt();

            if (in.hasNextLine()) {
                in.nextLine();
            }

            /* TODO */
            for (int i = 0; i < opponentActions; i++) {
                String cardNumberAndAction = in.nextLine();
            }

            int cardCount = in.nextInt();

            Card[] cards = new Card[cardCount];

            List<Card> myBoard = new ArrayList<>();
            List<Card> enemyBoard = new ArrayList<>();
            List<Card> myHand = new ArrayList<>();

            for (int i = 0; i < cardCount; i++) {
                int cardNumber = in.nextInt();
                int instanceId = in.nextInt();
                int location = in.nextInt();
                int cardType = in.nextInt();
                int cost = in.nextInt();
                int attack = in.nextInt();
                int defense = in.nextInt();
                String abilities = in.next();
                int myHealthChange = in.nextInt();
                int opponentHealthChange = in.nextInt();
                int cardDraw = in.nextInt();

                cards[i] = new Card(cardNumber, instanceId, location, cardType, cost, attack, defense, abilities, myHealthChange, opponentHealthChange, cardDraw);

                List<Card> destination;

                switch (location) {
                    case -1:
                        destination = enemyBoard;
                        break;

                    case 0:
                        destination = myHand;
                        break;

                    case 1:
                        cards[i].setCanAttack(true);
                        destination = myBoard;
                        break;

                    default:
                        destination = myHand;
                }

                destination.add(cards[i]);
            }

            Hero myHero = heroes[0];
            Hero enemyHero = heroes[1];

            myHero.setPlayerHand(myHand.size());
            enemyHero.setPlayerHand(opponentHand);

            Board board = new Board(myHero, enemyHero, myBoard, enemyBoard, myHand);

            //Draft
            if (myHero.getPlayerMana() <= 0) {
                if (amIFirst == null) amIFirst = myHero.getPlayerDeck() > enemyHero.getPlayerDeck();
                Card choice;
                List<Card> choices = new ArrayList<>();

                //Check my own hand first
                for (Card card : myHand) {
                    //if (card.getLocation() != 0)
                    {
                        //continue;
                    }

                    choices.add(card);
                }
                List<Card> originalChoices = new ArrayList<>(choices);
                Collections.sort(choices, Comparator.comparingInt(card -> card.evaluateDraft(draftHistory)));
                //Collections.reverse(choices);

                choice = choices.get(0);


                draftHistory.add(choice);
                output += "PICK " + originalChoices.indexOf(choice);
            } else {
                /*for (Card card : cards)
                {
                    //In Hand
                    if (card.getLocation() == 0)
                    {
                        if (card.getCost() <= myHero.getPlayerMana())
                        {
                            output += "SUMMON " + card.getInstanceId() + ";";
                            myHero.changePlayerMana(-card.getCost());
                        }
                    }

                }*/

                CommandGroup commands = new CommandGroup();
                HashMap<CommandGroup, Board> simResults = new HashMap<>();
                HashMap<CommandGroup, Board> simQueue = new HashMap<>();

                runSimulation(board, commands, simResults, simQueue);

                int limit = 200 - (int) Math.pow(board.getEntireBoard().size(), 2);
                //System.err.println("thing is " + limit);
                if (limit <= 50) limit = 50;

                //110000000
                while (System.nanoTime() - cTime < timeout && simQueue.size() > 0) {
//                    List<Map.Entry<CommandGroup, Board>> best = simQueue.entrySet().stream().collect(toList());
//                    Collections.shuffle(best);
//                    best = best.stream().limit(limit).collect(toList());

                    List<Map.Entry<CommandGroup, Board>> best = simQueue.entrySet().stream()
                            .sorted(comparing(x -> x.getValue().evaluate(), reverseOrder()))
                            .limit(limit)
                            .collect(toList());

//                    List<Map.Entry<CommandGroup, Board>> best = simQueue.entrySet().stream().collect(toList());

                    simQueue = new HashMap<>();

                    for (Map.Entry<CommandGroup, Board> entry : best) {
                        CommandGroup bestCommands = entry.getKey();
                        Board bestBoard = entry.getValue();

                        runSimulation(bestBoard, bestCommands, simResults, simQueue);
                    }
                    //System.err.println("Calculating the best out of " + simQueue.size() + " and " + simResults.size() + " are done");
                }

                //if(System.nanoTime() - cTime < 105000000)
                {
                    //System.err.println("has time " + (105000000 - (System.nanoTime() - cTime)) + " left: " + simResults.size());
                }

                if (simResults.size() > 0) {
                    //System.err.println("Selecting best move out of " + simResults.size() + " options");


                    Iterator it = simResults.entrySet().iterator();


                    HashMap.Entry<CommandGroup, Board> entry = ((HashMap.Entry<CommandGroup, Board>) it.next());
                    CommandGroup bestCommands = entry.getKey();
                    double highScore = entry.getValue().evaluatePrediction();

                    int counter = 0;
                    //System.err.println(counter + " times with " + (105000000 - (System.nanoTime() - cTime)) + "ns elapsed");

                    while (System.nanoTime() - cTime < timeout && it.hasNext()) {
                        ++counter;
                        HashMap.Entry<CommandGroup, Board> temp = ((HashMap.Entry<CommandGroup, Board>) it.next());
                        double score = temp.getValue().evaluatePrediction();
                        if (score > highScore) {
                            //System.err.println(counter + " times with " + (105000000 - (System.nanoTime() - cTime)) + "ns elapsed");

                            highScore = score;
                            bestCommands = temp.getKey();
                        }
                    }
                    //System.err.println(counter + " times with " + (105000000 - (System.nanoTime() - cTime)) + "ns elapsed");

                    //System.err.println();
                    //System.err.println("Selected best move out of " + simResults.size() + " options with " + (110000000 - (System.nanoTime() - cTime)) + "ns left");

                    //if(System.nanoTime() - cTime < 105000000)
                    {
                        //System.err.println("really has time" + (105000000 - (System.nanoTime() - cTime)));
                    }

                    //CommandGroup bestCommands = Collections.max(simResults.entrySet(), (board1, board2) -> (int) (board1.getValue().evaluatePrediction() - board2.getValue().evaluatePrediction())).getKey();
                    //CommandGroup bestCommands = Collections.max(simResults.entrySet(), (board1, board2) -> (int) (board1.getValue().evaluate() - board2.getValue().evaluate())).getKey();
                    output += bestCommands.toString();
                }

            }

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");

            System.out.println(output.isEmpty() ? "PASS" : output);
        }
    }

    public static void runSimulation(Board board, CommandGroup commands, HashMap<CommandGroup, Board> simResults, HashMap<CommandGroup, Board> simQueue) {
        //max hand  - 8
        //max board - 6

        //base case
        //if (!board.canPlay() || simResults.values().stream().filter(x -> x.evaluate() > 9999999).count() > 0)
        {
            simResults.put(commands, board);

            //System.err.println("ON MY " + simResults.size());
            //return;
        }

        Board originalBoard = new Board(board);
        CommandGroup originalCommands = new CommandGroup(commands);

        board = originalBoard;
        commands = originalCommands;

        double originalValue = originalBoard.evaluate();

        //trading
        boolean hasTaunt = board.getEnemyBoard().stream().anyMatch(x -> x.hasGuard());


        for (Card minion : board.getMyBoard()) {
            if (!minion.canAttack())// || minion.getAttack() <= 0)
            {
                continue;
            }
            if (!hasTaunt) {
                Board tempBoard = new Board(board);
                CommandGroup tempCommands = new CommandGroup(commands);
                Card tempMinion = tempBoard.getMyBoard().get(board.getMyBoard().indexOf(minion));

                tempMinion.setCanAttack(false);
                tempCommands.addCommand(new Command(Command.ATTACK, tempMinion));
                tempBoard.simulateTrade(tempMinion, null);

                //if(tempBoard.evaluate() > originalValue)
                {
                    //runSimulation(mana, tempBoard, tempCommands, simResults);
                    simQueue.put(tempCommands, tempBoard);
                }

                //tempBoard = originalBoard;
                //commands = originalCommands;
            }
            for (Card enemy : board.getEnemyBoard()) {
                if (hasTaunt && !enemy.hasGuard()) {
                    continue;
                }

                Board tempBoard = new Board(board);
                CommandGroup tempCommands = new CommandGroup(commands);
                Card tempMinion = tempBoard.getMyBoard().get(board.getMyBoard().indexOf(minion));
                Card tempEnemy = tempBoard.getEnemyBoard().get(board.getEnemyBoard().indexOf(enemy));

                tempMinion.setCanAttack(false);
                tempCommands.addCommand(new Command(Command.ATTACK, tempMinion, tempEnemy));
                tempBoard.simulateTrade(tempMinion, tempEnemy);

                //if(hasTaunt || tempBoard.evaluate() > originalValue)
                {
                    //runSimulation(mana, tempBoard, tempCommands, simResults);
                    simQueue.put(tempCommands, tempBoard);
                }
                //board = originalBoard;
                //commands = originalCommands;
            }
        }

        //playing cards
        boolean fullBoard = board.getMyBoard().size() >= 6;

        for (Card card : board.getMyHand()) {
            //Cost Check
            if (board.getMyHero().getPlayerMana() < card.getCost()) {
                continue;
            }
            if (card.getCardType() == Card.CREATURE) {
                //Board Space Check
                if (fullBoard) {
                    continue;
                }
                Board tempBoard = new Board(board);
                CommandGroup tempCommands = new CommandGroup(commands);
                Card tempMinion = tempBoard.getMyHand().get(board.getMyHand().indexOf(card));

                tempCommands.addCommand(new Command(Command.SUMMON, tempMinion));
                tempBoard.simulateSummon(tempMinion);

                //if (tempBoard.evaluate() > originalValue)
                {
                    //runSimulation(mana - card.getCost(), tempBoard, tempCommands, simResults);
                    simQueue.put(tempCommands, tempBoard);
                }
                //board = originalBoard;
                //commands = originalCommands;
            }

            if (card.getCardType() == Card.GREEN) {
                for (Card minion : board.getMyBoard()) {
                    Board tempBoard = new Board(board);
                    CommandGroup tempCommands = new CommandGroup(commands);
                    Card tempItem = tempBoard.getMyHand().get(board.getMyHand().indexOf(card));
                    Card tempMinion = tempBoard.getMyBoard().get(board.getMyBoard().indexOf(minion));

                    tempCommands.addCommand(new Command(Command.USE, tempItem, tempMinion));
                    tempBoard.simulateItem(tempItem, tempMinion);

                    //if (tempBoard.evaluate() > originalValue)
                    {
                        //runSimulation(mana - card.getCost(), tempBoard, tempCommands, simResults);
                        simQueue.put(tempCommands, tempBoard);
                    }
                    //board = originalBoard;
                    //commands = originalCommands;
                }
            }

            if (card.getCardType() == Card.RED) {
                for (Card enemy : board.getEnemyBoard()) {
                    Board tempBoard = new Board(board);
                    CommandGroup tempCommands = new CommandGroup(commands);
                    Card tempItem = tempBoard.getMyHand().get(board.getMyHand().indexOf(card));
                    Card tempMinion = tempBoard.getEnemyBoard().get(board.getEnemyBoard().indexOf(enemy));

                    tempCommands.addCommand(new Command(Command.USE, tempItem, tempMinion));
                    tempBoard.simulateItem(tempItem, tempMinion);

                    //if (tempBoard.evaluate() > originalValue)
                    {
                        //runSimulation(mana - card.getCost(), tempBoard, tempCommands, simResults);
                        simQueue.put(tempCommands, tempBoard);
                    }
                    //board = originalBoard;
                    //commands = originalCommands;
                }
            }

            if (card.getCardType() == Card.BLUE) {
                Board tempBoard = new Board(board);
                CommandGroup tempCommands = new CommandGroup(commands);
                Card tempItem = tempBoard.getMyHand().get(board.getMyHand().indexOf(card));

                tempCommands.addCommand(new Command(Command.USE, tempItem));
                tempBoard.simulateItem(tempItem, null);

                //if (tempBoard.evaluate() > originalValue)
                {
                    //runSimulation(mana - card.getCost(), tempBoard, tempCommands, simResults);
                    simQueue.put(tempCommands, tempBoard);
                }
                //board = originalBoard;
                //commands = originalCommands;

            }
        }

    }

    public static void runCounterSimulation(Board board, List<Board> simResults, List<Board> simQueue) {
        simResults.add(board);

        //trading
        boolean hasTaunt = board.getEnemyBoard().stream().anyMatch(x -> x.hasGuard());

        for (Card minion : board.getMyBoard()) {
            if (!minion.canAttack())// || minion.getAttack() <= 0)
            {
                continue;
            }
            if (!hasTaunt) {
                Board tempBoard = new Board(board);
                Card tempMinion = tempBoard.getMyBoard().get(board.getMyBoard().indexOf(minion));

                tempMinion.setCanAttack(false);
                tempBoard.simulateTrade(tempMinion, null);

                //if(tempBoard.evaluate() > originalValue)
                {
                    //runSimulation(mana, tempBoard, tempCommands, simResults);
                    simQueue.add(tempBoard);
                }

                //tempBoard = originalBoard;
                //commands = originalCommands;
            }
            for (Card enemy : board.getEnemyBoard()) {
                if (hasTaunt && !enemy.hasGuard()) {
                    continue;
                }

                Board tempBoard = new Board(board);
                Card tempMinion = tempBoard.getMyBoard().get(board.getMyBoard().indexOf(minion));
                Card tempEnemy = tempBoard.getEnemyBoard().get(board.getEnemyBoard().indexOf(enemy));

                tempMinion.setCanAttack(false);
                tempBoard.simulateTrade(tempMinion, tempEnemy);

                //if(hasTaunt || tempBoard.evaluate() > originalValue)
                {
                    //runSimulation(mana, tempBoard, tempCommands, simResults);
                    simQueue.add(tempBoard);
                }
                //board = originalBoard;
                //commands = originalCommands;
            }
        }
    }


}

class Command {
    public static final int PASS = 0;
    public static final int SUMMON = 1;
    public static final int ATTACK = 2;
    public static final int USE = 3;
    public static final int DRAFT = 4;

    int type;
    Card card1 = null, card2 = null;
    boolean attackOp = false;

    public Command() { // CONSTRUCTOR FOR PASS
        this.type = Command.PASS;
    }

    public Command(int type) {
        this.type = type;
    }

    public Command(int type, Card card1) {
        this.type = type;
        this.card1 = card1;
        if (this.type == Command.ATTACK || this.type == Command.USE) {
            attackOp = true;
        }
    }

    public Command(int type, Card card1, Card card2) {
        this.type = type;
        this.card1 = card1;
        this.card2 = card2;
    }

    @Override
    public String toString() {
        String str;
        switch (type) {
            case Command.PASS:
                str = "PASS";
                return str;
            case Command.ATTACK:
                str = "ATTACK";
                break;
            case Command.USE:
                str = "USE";
                break;
            case Command.DRAFT:
                str = "PICK " + card1.getInstanceId();
                return str;
            case Command.SUMMON:
                str = "SUMMON";
                break;
            default:
                str = "PASS";
                return str;
        }
        str += " " + card1.getInstanceId();
        if (card2 == null) {
            if (attackOp) {
                str += " -1";
            }
        } else {
            str += " " + card2.getInstanceId();
        }
        str += ";";
        return str;
    }
}

class CommandGroup {
    private List<Command> commands;

    public CommandGroup() {
        this.commands = new ArrayList<>();
    }

    public CommandGroup(List<Command> commands) {
        this.commands = new ArrayList<>();
        for (Command c : commands) {
            this.commands.add(c);
        }
    }

    public CommandGroup(CommandGroup commands) {
        this.commands = new ArrayList<>();
        for (Command c : commands.getCommands()) {
            this.commands.add(c);
        }
    }

    public void addCommand(Command command) {
        commands.add(command);
    }

    public List<Command> getCommands() {
        return this.commands;
    }

    @Override
    public String toString() {
        String str = "";
        for (Command c : commands) {
            str += c.toString();
        }
        return str;
    }
}

class Hero {
    private int playerHealth;
    private int playerMana;
    private int playerDeck;
    private int playerRune;
    private int playerHand;

    public Hero(int playerHealth, int playerMana, int playerDeck, int playerRune) {
        this(playerHealth, playerMana, playerDeck, playerRune, 0);
    }

    public Hero(int playerHealth, int playerMana, int playerDeck, int playerRune, int playerHand) {
        this.playerHealth = playerHealth;
        this.playerMana = playerMana;
        this.playerDeck = playerDeck;
        this.playerRune = playerRune;
        this.playerHand = playerHand;
    }

    public Hero(Hero hero) {
        this.playerHealth = hero.getPlayerHealth();
        this.playerMana = hero.getPlayerMana();
        this.playerDeck = hero.getPlayerDeck();
        this.playerRune = hero.getPlayerRune();
        this.playerHand = hero.getPlayerHand();
    }

    public int getPlayerHealth() {
        return playerHealth;
    }

    public void setPlayerHealth(int playerHealth) {
        this.playerHealth = playerHealth;
    }

    public void changePlayerHealth(int change) {
        this.playerHealth += change;
    }

    public int getPlayerMana() {
        return playerMana;
    }

    public void setPlayerMana(int playerMana) {
        this.playerMana = playerMana;
    }

    public void changePlayerMana(int change) {
        this.playerMana += change;
    }

    public int getPlayerDeck() {
        return playerDeck;
    }

    public void setPlayerDeck(int playerDeck) {
        this.playerDeck = playerDeck;
    }

    public void changePlayerDeck(int change) {
        this.playerDeck += change;
        this.playerHand -= change;
    }

    public int getPlayerRune() {
        return playerRune;
    }

    public void setPlayerRune(int playerRune) {
        this.playerRune = playerRune;
    }

    public void changePlayerRune(int change) {
        this.playerRune += change;
    }

    public int getPlayerHand() {
        return playerHand;
    }

    public void setPlayerHand(int playerHand) {
        this.playerHand = playerHand;
    }

    public void playCard() {
        this.playerHand--;
    }

    public double evaluate() {
        double value = 0;

        double healthMod;

        if (Player.amIFirst) {
            healthMod = 0.4;
        } else {
            healthMod = 0.4;
        }
//
//        if (this.playerHealth < 15)
//        {
//            healthMod *= 0.5;
//        }

        value += Math.pow(this.playerHealth, 1) * healthMod;

        return value;
//        double value = 0;
//
//        double healthMod = 0.4;
//
//        if (this.playerHealth < 5)
//        {
//            healthMod *= 0.5;
//        }
//
//        value += this.playerHealth * healthMod;
//
//        return value;
    }
}

class Card {
    public static final int CREATURE = 0;
    public static final int GREEN = 1;
    public static final int RED = 2;
    public static final int BLUE = 3;

    //private static final int[] priority = {116, 68, 151, 51, 65, 80, 7, 53, 29, 37, 67, 32, 139, 69, 49, 33, 66, 147, 18, 152, 28, 48, 82, 88, 23, 84, 52, 44, 87, 148, 99, 121, 64, 85, 103, 141, 158, 50, 95, 115, 133, 19, 109, 54, 157, 81, 150, 21, 34, 36, 135, 134, 70, 3, 61, 111, 75, 17, 144, 129, 145, 106, 9, 105, 15, 114, 128, 155, 96, 11, 8, 86, 104, 97, 41, 12, 26, 149, 90, 6, 13, 126, 93, 98, 83, 71, 79, 72, 73, 77, 59, 100, 137, 5, 89, 142, 112, 25, 62, 125, 122, 74, 120, 159, 22, 91, 39, 94, 127, 30, 16, 146, 1, 45, 38, 56, 47, 4, 58, 118, 119, 40, 27, 35, 101, 123, 132, 2, 136, 131, 20, 76, 14, 43, 102, 108, 46, 60, 130, 117, 140, 42, 124, 24, 63, 10, 154, 78, 31, 57, 138, 107, 113, 55, 143, 92, 156, 110, 160, 153};
    private static final int[] priority = {68, 116, 7, 80, 65, 51, 49, 48, 66, 67, 23, 61, 115, 69, 44, 37, 52, 53, 18, 29, 50, 54, 82, 32, 114, 95, 79, 64, 99, 111, 33, 84, 3, 62, 103, 11, 96, 97, 77, 139, 59, 36, 109, 81, 19, 9, 87, 105, 121, 6, 8, 85, 93, 5, 91, 137, 70, 88, 28, 112, 83, 21, 34, 46, 106, 26, 104, 22, 17, 15, 1, 72, 75, 98, 45, 90, 58, 25, 13, 39, 133, 12, 35, 100, 41, 16, 118, 30, 129, 119, 60, 135, 38, 71, 76, 134, 94, 126, 74, 43, 122, 56, 128, 27, 73, 127, 40, 4, 14, 125, 20, 2, 47, 120, 101, 78, 108, 31, 89, 86, 42, 10, 102, 132, 24, 136, 138, 130, 107, 123, 117, 113, 131, 57, 124, 63, 92, 55, 140, 110, 144, 145, 147, 142, 149, 158, 146, 152, 155, 154, 151, 157, 143, 156, 159, 153, 141, 150, 148, 160};
    //private static final int[] priority = {68, 116, 7, 51, 49, 80, 65, 66, 48, 67, 61, 115, 44, 23, 37, 69, 53, 52, 50, 18, 29, 54, 32, 82, 144, 147, 114, 79, 99, 145, 64, 95, 84, 111, 11, 33, 3, 103, 109, 139, 9, 36, 158, 149, 142, 154, 97, 62, 152, 77, 88, 96, 146, 85, 59, 81, 155, 87, 6, 1, 34, 8, 105, 46, 106, 72, 5, 121, 19, 110, 157, 83, 26, 58, 91, 28, 93, 17, 90, 156, 159, 151, 143, 129, 104, 41, 118, 137, 133, 98, 153, 39, 112, 21, 70, 75, 12, 94, 15, 16, 135, 100, 35, 160, 25, 45, 126, 31, 30, 150, 148, 141, 22, 119, 38, 74, 4, 78, 127, 27, 47, 76, 43, 134, 122, 120, 13, 14, 60, 125, 71, 56, 128, 73, 40, 101, 42, 89, 20, 138, 108, 10, 123, 86, 24, 2, 107, 132, 102, 130, 117, 113, 136, 131, 63, 57, 124, 92, 55, 140};
    //private static final int[] priority = {68, 51, 80, 49, 116, 7, 65, 115, 52, 66, 67, 48, 37, 32, 61, 44, 69, 50, 18, 87, 23, 54, 64, 33, 114, 79, 95, 3, 103, 77, 6, 53, 82, 84, 129, 111, 85, 72, 25, 97, 1, 99, 109, 62, 83, 139, 9, 34, 5, 104, 41, 39, 11, 88, 59, 8, 121, 29, 105, 26, 112, 12, 22, 118, 21, 45, 78, 36, 81, 58, 70, 75, 127, 138, 91, 94, 108, 96, 46, 19, 137, 15, 126, 31, 27, 47, 28, 133, 119, 43, 73, 93, 17, 38, 60, 86, 16, 30, 74, 76, 125, 40, 98, 134, 24, 135, 122, 101, 106, 71, 42, 117, 100, 4, 2, 113, 89, 10, 136, 131, 35, 120, 20, 56, 123, 13, 128, 63, 57, 130, 90, 124, 132, 107, 14, 92, 102, 140, 55, 110, 155, 158, 150, 152, 156, 145, 159, 151, 146, 143, 160, 149, 153, 148, 142, 144, 154, 157, 147, 141};

    private int cardNumber;
    private int instanceId;
    private int location;
    private int cardType;
    private int cost;
    private int attack;
    private int defense;
    private int myHealthChange;
    private int opponentHealthChange;
    private int cardDraw;
    private String abilities;
    private boolean canAttack;

    private boolean breakthrough;
    private boolean charge;
    private boolean drain;
    private boolean guard;
    private boolean lethal;
    private boolean ward;

    public Card(int cardNumber, int instanceId, int location, int cardType, int cost, int attack, int defense, String abilities, int myHealthChange, int opponentHealthChange, int cardDraw) {
        this.cardNumber = cardNumber;
        this.instanceId = instanceId;
        this.location = location;
        this.cardType = cardType;
        this.cost = cost;
        this.attack = attack;
        this.defense = defense;
        this.abilities = abilities;
        this.myHealthChange = myHealthChange;
        this.opponentHealthChange = opponentHealthChange;
        this.cardDraw = cardDraw;
        canAttack = false;

        setBreakthrough(abilities.contains("B"));
        setCharge(abilities.contains("C"));
        setDrain(abilities.contains("D"));
        setGuard(abilities.contains("G"));
        setLethal(abilities.contains("L"));
        setWard(abilities.contains("W"));
    }

    public Card(Card card) {
        this(card, card.canAttack);
    }

    public Card(Card card, boolean canAttack) {
        this.cardNumber = card.getCardNumber();
        this.instanceId = card.getInstanceId();
        this.location = card.getLocation();
        this.cardType = card.getCardType();
        this.cost = card.getCost();
        this.attack = card.getAttack();
        this.defense = card.getDefense();
        this.abilities = card.getAbilities();
        this.myHealthChange = card.getMyHealthChange();
        this.opponentHealthChange = card.getOpponentHealthChange();
        this.cardDraw = card.getCardDraw();
        this.canAttack = canAttack;

        setBreakthrough(abilities.contains("B"));
        setCharge(abilities.contains("C"));
        setDrain(abilities.contains("D"));
        setGuard(abilities.contains("G"));
        setLethal(abilities.contains("L"));
        setWard(abilities.contains("W"));
    }

    public int getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(int cardNumber) {
        this.cardNumber = cardNumber;
    }

    public int getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(int instanceId) {
        this.instanceId = instanceId;
    }

    public int getLocation() {
        return location;
    }

    public void setLocation(int location) {
        this.location = location;
    }

    public int getCardType() {
        return cardType;
    }

    public void setCardType(int cardType) {
        this.cardType = cardType;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public int getAttack() {
        return attack;
    }

    public void setAttack(int attack) {
        this.attack = attack;
    }

    public int getDefense() {
        return defense;
    }

    public void setDefense(int defense) {
        this.defense = defense;
    }

    public int getMyHealthChange() {
        return myHealthChange;
    }

    public void setMyHealthChange(int myHealthChange) {
        this.myHealthChange = myHealthChange;
    }

    public int getOpponentHealthChange() {
        return opponentHealthChange;
    }

    public void setOpponentHealthChange(int opponentHealthChange) {
        this.opponentHealthChange = opponentHealthChange;
    }

    public int getCardDraw() {
        return cardDraw;
    }

    public void setCardDraw(int cardDraw) {
        this.cardDraw = cardDraw;
    }

    public String getAbilities() {
        return abilities;
    }

    public void setAbilities(String abilities) {
        this.abilities = abilities;
    }

    public boolean canAttack() {
        return this.canAttack;
    }

    public void setCanAttack(boolean bool) {
        this.canAttack = bool;
    }

    public void attack(Card enemyCard) {
        if (!enemyCard.hasWard()) {
            if (this.hasLethal()) {
                enemyCard.setDefense(0);
            } else {
                enemyCard.setDefense(enemyCard.getDefense() - this.getAttack());
            }
        }
        enemyCard.setWard(false);
    }

    public double evaluate() {
        //Adjust Values
        double modAttack = 2;
        double modDefense = 2;

        double modBreakthrough = 0.1;
        double modCharge = 0;
        double modDrain = 0.1;
        double modGuard = 0.8;
        double modLethal = 8;
        double modWard = 2;

        double modHeal = 0;
        double modFace = 0;
        double modCardDraw = 0;

        double modItemRatio = 0.75;
        double modGuardRatio = 1.5;
        double modEnemyRatio = 1.8;

        //Once in play for 1 turn...
        if (Player.amIFirst) {
            if (this.getLocation() == 1) {
                modCharge = 0;
                modHeal = 0;
                modFace = 0;
                modCardDraw = 0;

                modAttack = 1.35;
                modDefense = 0;
                modLethal = 5;
                modWard = 3;
            }
            if (this.getLocation() == -1) {
                modCharge = 0;
                modHeal = 0;
                modFace = 0;
                modCardDraw = 0;
                modGuard = 0.1;

                modAttack = 1.1;
                modDefense = 0;
                modLethal = 11;
                modWard = 2;
            }
        } else {
            if (this.getLocation() == 1) {
                modCharge = 0;
                modHeal = 0;
                modFace = 0;
                modCardDraw = 0;

                modAttack = 1.3;
                modDefense = 0;
                modLethal = 7;
                modWard = 2;
            }
            if (this.getLocation() == -1) {
                modCharge = 0;
                modHeal = 0;
                modFace = 0;
                modCardDraw = 0;
                modGuard = 0.6;

                modAttack = 1.1;
                modDefense = 0;
                modLethal = 1;
                modWard = 2;
            }
        }

        if (this.getCardType() == Card.RED) {
            modAttack *= -1;
            modDefense *= -1;
            modBreakthrough *= -1;
            modCharge *= -1;
            modGuard *= -1;
            modDrain *= -1;
        }
        if (this.getCardType() == Card.BLUE) {
            modDefense *= -1;
        }

        double value = 0;
        value += this.getAttack() * modAttack;
        value += this.getDefense() * modDefense;
        //value += this.getCost() * modCost;
        //value += this.getMyHealthChange() * modHeal;
        //value += this.getOpponentHealthChange() * modFace;
        //value += this.getCardDraw() * modCardDraw;

        value += this.hasBreakthrough() ? modBreakthrough * this.getAttack() : 0;
        value += this.hasCharge() ? modCharge * this.getAttack() : 0;
        value += this.hasDrain() ? modDrain * this.getAttack() : 0;
        value += this.hasGuard() ? modGuard * this.getDefense() : 0;
        value += this.hasLethal() ? modLethal : 0;
        value += this.hasWard() ? modWard * this.getAttack() : 0;

        value *= this.hasGuard() ? modGuardRatio : 1;

        if (this.getCardType() != Card.CREATURE) {
            value *= modItemRatio;
        }

        if (this.getLocation() == -1) {
            value *= this.hasGuard() ? modGuardRatio : 1;
            value *= modEnemyRatio;
        }

//        if(Player.amIFirst)
//        {
//            value *= 0.5;
//        }
//        else
//        {
//            value *= 0.75;
//        }

        return value;
    }

    public int evaluateDraft(List<Card> draft) {
        List<Integer> prioList = Arrays.stream(priority).boxed().collect(Collectors.toList());
        int value = prioList.indexOf(this.cardNumber);
        long count = draft.stream().filter(x -> x.getCost() > 5).count();
        long ones = draft.stream().filter(x -> x.getCost() == 1 && x.getCardType() == Card.CREATURE).count();
        long targetOnes = Player.amIFirst ? 1 : 2;
        if (draft.size() > 8) {
            if (value > 10) {
                double sum = this.getCost();
                for (Card c : draft) {
                    sum += c.getCost();
                }
                double avg = sum / (draft.size() + 1);
                double targetAvg = Player.amIFirst ? 4.5 : 3.5;
                if (avg > targetAvg) {
                    if (Player.amIFirst) {
                        value += 1.5 * Math.pow(this.getCost() - targetAvg, 2);
                    } else {
                        value += 3 * Math.pow(this.getCost() - targetAvg, 2);
                    }
                } else {
                    if (Player.amIFirst) {
                        value += 3 * Math.pow(targetAvg - this.getCost(), 2);
                    } else {
                        value += 1.5 * Math.pow(targetAvg - this.getCost(), 2);
                    }
                }
            }
            if (this.getCost() == 1 && this.getCardType() == Card.CREATURE) {
                if (ones <= targetOnes) {
                    value -= 15;
                }
            }
            if (this.getCost() <= 5) {
                if (Player.amIFirst) {
                    //if(count > 20) count = 20;
                    value -= count * 3;
                } else {
                    value -= count * 6;
                }
            }
        }

        return value;
//        int count = draft.size();
//        int minionCount = (int) draft.stream().filter(x -> x.getCardType() == Card.CREATURE).count();
//        int itemCount = count - minionCount;
//
//        final int MAXITEMS = 15;
//
//        double score = this.evaluate();
//
//        if (this.getCardType() != Card.CREATURE)
//        {
//            if (itemCount >= MAXITEMS)
//            {
//                score -= 10000;
//            }
//        }
//
//
//        if(draft.size() > 5)
//        {
//            int effectiveCost = this.getCost() > 7 ? 7 : this.getCost();
//
//            //higher value = more preferred
//            int[] curve = new int[] {1,3,6,4,3,3,2,2};
//
//            for(Card card : draft)
//            {
//                int tempCost = card.getCost() > 7 ? 7 : card.getCost();
//                curve[tempCost]--;
//            }
//
//            //System.err.println("We already have " + curve[effectiveCost] + " of " + effectiveCost + " cost minions so adding score by " + (curve[effectiveCost] * 10) + " to a score of " + (score + curve[effectiveCost]));
//
//            score += curve[effectiveCost] * 2;
//        }
//
//        return score;
    }

    public boolean hasBreakthrough() {
        return breakthrough;
    }

    public void setBreakthrough(boolean breakthrough) {
        this.breakthrough = breakthrough;
    }

    public boolean hasCharge() {
        return charge;
    }

    public void setCharge(boolean charge) {
        this.charge = charge;
    }

    public boolean hasDrain() {
        return drain;
    }

    public void setDrain(boolean drain) {
        this.drain = drain;
    }

    public boolean hasGuard() {
        return guard;
    }

    public void setGuard(boolean guard) {
        this.guard = guard;
    }

    public boolean hasLethal() {
        return lethal;
    }

    public void setLethal(boolean lethal) {
        this.lethal = lethal;
    }

    public boolean hasWard() {
        return ward;
    }

    public void setWard(boolean ward) {
        this.ward = ward;
    }
}

class Board {
    public static final int ENEMY_BOARD = -1;
    public static final int MY_HAND = 0;
    public static final int MY_BOARD = 1;

    private Hero myHero;
    private Hero enemyHero;
    private List<Card> myBoard;
    private List<Card> enemyBoard;
    private List<Card> myHand;

    public Board(Hero myHero, Hero enemyHero, List<Card> myBoard, List<Card> enemyBoard, List<Card> myHand) {
        this.myHero = myHero;
        this.enemyHero = enemyHero;
        this.myBoard = myBoard;
        this.enemyBoard = enemyBoard;
        this.myHand = myHand;
    }

    public Board(Board board) {
        this(board, false);
    }

    public Board(Board board, boolean flip) {
        if (flip) {
            this.myHero = new Hero(board.getEnemyHero());
            this.enemyHero = new Hero(board.getMyHero());

            this.myBoard = new ArrayList<>();
            for (Card c : board.getEnemyBoard()) {
                Card nc = new Card(c, true);
                this.myBoard.add(nc);
            }

            this.enemyBoard = new ArrayList<>();
            for (Card c : board.getMyBoard()) {
                Card nc = new Card(c, false);
                this.enemyBoard.add(nc);
            }

            this.myHand = new ArrayList<>();
        } else {
            this.myHero = new Hero(board.getMyHero());
            this.enemyHero = new Hero(board.getEnemyHero());

            this.myBoard = new ArrayList<>();
            for (Card c : board.getMyBoard()) {
                Card nc = new Card(c);
                this.myBoard.add(nc);
            }

            this.enemyBoard = new ArrayList<>();
            for (Card c : board.getEnemyBoard()) {
                Card nc = new Card(c);
                this.enemyBoard.add(nc);
            }

            this.myHand = new ArrayList<>();
            for (Card c : board.getMyHand()) {
                Card nc = new Card(c);
                this.myHand.add(nc);
            }
        }
    }

    public List<Card> getMyBoard() {
        return this.myBoard;
    }

    public List<Card> getEnemyBoard() {
        return this.enemyBoard;
    }

    public List<Card> getEntireBoard() {
        return Stream.concat(myBoard.stream(), enemyBoard.stream()).collect(toList());
    }

    public void simulateTrade(Card myCard, Card enemyCard) {
        if (enemyCard == null) {
            getEnemyHero().changePlayerHealth(-myCard.getAttack());
            return;
        } else {
            //myCard = tempBoard.getMyBoard().get(myBoard.indexOf(myCard));
            //enemyCard = tempBoard.getEnemyBoard().get(enemyBoard.indexOf(enemyCard));

            myCard.attack(enemyCard);
            enemyCard.attack(myCard);

            if (myCard.getDefense() <= 0) {
                getMyBoard().remove(myCard);
            }
            if (enemyCard.getDefense() <= 0) {
                getEnemyBoard().remove(enemyCard);
            }

            return;
        }
    }

    public void simulateItem(Card item, Card target) {
        //myCard = tempBoard.getMyBoard().get(myBoard.indexOf(myCard));
        //enemyCard = tempBoard.getEnemyBoard().get(enemyBoard.indexOf(enemyCard));

        this.getMyHand().remove(item);
        this.getMyHero().changePlayerMana(-item.getCost());

        int cardType = item.getCardType();
        boolean buff = true;

        this.getMyHero().changePlayerHealth(item.getMyHealthChange());
        this.getEnemyHero().changePlayerHealth(item.getOpponentHealthChange());
        this.getMyHero().changePlayerDeck(-item.getCardDraw());

        if (target != null) {
            if (cardType == Card.RED) {
                buff = false;
            }

            target.setAttack(target.getAttack() + item.getAttack());
            target.setDefense(target.getDefense() + item.getDefense());

            if (item.hasBreakthrough()) {
                target.setBreakthrough(buff);
            }
            if (item.hasCharge()) {
                target.setCharge(buff);
            }
            if (item.hasDrain()) {
                target.setDrain(buff);
            }
            if (item.hasLethal()) {
                target.setLethal(buff);
            }
            if (item.hasGuard()) {
                target.setGuard(buff);
            }
            if (item.hasWard()) {
                target.setWard(buff);
            }
        } else {
            this.getEnemyHero().changePlayerHealth(item.getDefense());
        }
    }

    public void simulateSummon(Card minion) {
        if (minion.getLocation() != Board.MY_HAND) {
            return;
        }

        if (minion.hasCharge()) {
            minion.setCanAttack(true);
        }

        //minion.setLocation(1);
        this.getMyBoard().add(minion);
        this.getMyHand().remove(minion);
        this.getMyHero().changePlayerMana(-minion.getCost());
        this.getMyHero().changePlayerDeck(-minion.getCardDraw());

        this.getMyHero().changePlayerHealth(minion.getMyHealthChange());
        this.getEnemyHero().changePlayerHealth(minion.getOpponentHealthChange());
    }


    public double evaluate() {
        double value = 0;

        int possibleDeath = myHero.getPlayerHealth();
        int possibleLethal = enemyHero.getPlayerHealth();

        while (myHero.getPlayerHealth() < myHero.getPlayerRune()) {
            myHero.changePlayerDeck(-1);
            myHero.changePlayerRune(-5);
        }

        while (enemyHero.getPlayerHealth() < enemyHero.getPlayerRune()) {
            enemyHero.changePlayerDeck(-1);
            enemyHero.changePlayerRune(-5);
        }

        if (possibleDeath <= 0) {
            return -99999999;
        }

        int myMinionCount = 0;
        int enemyMinionCount = 0;

        //value += myHero.evaluate();
        if (Player.amIFirst) {
            value += (myHero.getPlayerHealth() - enemyHero.getPlayerHealth()) * 0.4;
        } else {
            value += (myHero.getPlayerHealth() - enemyHero.getPlayerHealth() * 3) * 0.4;
        }
        for (Card card : myBoard) {
            value += card.evaluate();
            myMinionCount++;
        }
        if (myHero.getPlayerDeck() <= 0) {
            possibleDeath = myHero.getPlayerRune();
        }

        //value -= enemyHero.evaluate();
        for (Card card : enemyBoard) {
            possibleDeath -= card.getAttack();
            value -= (card.evaluate() + 11);
            enemyMinionCount++;
        }
//        if(enemyHero.getPlayerDeck() <= 0)
//        {
//            possibleLethal = enemyHero.getPlayerRune();
//        }

        if (!Player.amIFirst) {
            if (enemyHero.getPlayerDeck() < 13) {
                value -= (Math.log(enemyHero.getPlayerHand())) * 5;
            }
        }


        if (possibleDeath <= 0) {
            value -= 9999999;
        }
        if (possibleLethal <= 0) {
            value += 99999999;
        }

        value += (myMinionCount - enemyMinionCount) * 3;
        value += (myHero.getPlayerHand() - enemyHero.getPlayerHand()) * 3;
        value += (myHero.getPlayerDeck() - enemyHero.getPlayerDeck()) * 3;

        return value;
    }

    public double evaluatePrediction() {
        double bestValue = this.evaluate();

        Board tempBoard = new Board(this, true);
        List<Board> simResults = new ArrayList<>();
        List<Board> simQueue = new ArrayList<>();
        List<Board> best = new ArrayList<>();
        Player.runCounterSimulation(tempBoard, simResults, simQueue);

        int limit = 80;

        while (simQueue.size() > 0) {
            //best = simQueue.stream().sorted(comparing(x->x.evaluate(), reverseOrder())).limit(limit).collect(toList());
            Collections.shuffle(simQueue);
            best = simQueue.stream().limit(limit).collect(toList());
            simQueue = new ArrayList<>();

            for (Board board : best) {
                Player.runCounterSimulation(board, simResults, simQueue);
            }
        }

        if (simResults.size() > 0) {
            Board bestBoard = Collections.max(simResults, (board1, board2) -> (int) (board1.evaluate() - board2.evaluate()));
            //bestBoard = new Board(bestBoard, true);
            bestValue = -bestBoard.evaluate();
        }

        return bestValue;
    }


    public boolean canPlay() {
        for (Card minion : myBoard) {
            if (minion.canAttack()) {
                return true;
            }
        }
        if (myBoard.size() < 6) {
            for (Card card : myHand) {
                if (card.getCost() <= this.getMyHero().getPlayerMana()) {
                    return true;
                }
            }
        }
        return false;
    }

    public Hero getMyHero() {
        return myHero;
    }

    public void setMyHero(Hero myHero) {
        this.myHero = myHero;
    }

    public Hero getEnemyHero() {
        return enemyHero;
    }

    public void setEnemyHero(Hero enemyHero) {
        this.enemyHero = enemyHero;
    }

    public List<Card> getMyHand() {
        return myHand;
    }

    public void setMyHand(List<Card> myHand) {
        this.myHand = myHand;
    }
}