import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

//represents a Game of minesweeper
class Minesweeper extends World {
  ArrayList<ArrayList<Cell>> rowsOfCells;
  Random rand;
  int cellsRevealed;

  static final int GRID_WIDTH = 33; //represents the number of columns of cells
  static final int GRID_HEIGHT = 16; //represents the number of rows of cells
  static final int CELL_SIZE = 50; // represents the cell size in pixels
  static final int SCREEN_HEIGHT = 
      CELL_SIZE * GRID_HEIGHT; // represents the height of the window in pixels
  static final int SCREEN_WIDTH = 
      CELL_SIZE * GRID_WIDTH; // represents the width of the window in pixels
  static final int NUM_MINES = 40; //represents the number of mines to generate
  static final Color CELL_UCOLOR = Color.GRAY; //represents the unrevealed cell's color
  static final Color CELL_RCOLOR = Color.DARK_GRAY; //represents the revealed cell's color
  static final WorldScene EMPTY_SCENE = new WorldScene(SCREEN_WIDTH, SCREEN_HEIGHT);
  static final ArrayList<Color> VALUE_COLORS = new ArrayList<Color>(
      Arrays.asList(CELL_RCOLOR, Color.BLUE, Color.GREEN, Color.RED, new Color(0, 0, 153),
          new Color(102, 51, 0), Color.CYAN, Color.BLACK, Color.LIGHT_GRAY));



  //constructor for tests
  Minesweeper(Random rand, boolean populate) {
    this.rand = rand;
    this.cellsRevealed = 0;
    //create cells
    this.initCells(GRID_WIDTH, GRID_HEIGHT, NUM_MINES);
    //add neighbors
    if (populate) {
      this.populateNeighbors(GRID_HEIGHT, GRID_WIDTH);
    }
    //count bombs/assign values
    this.countMines();
  }

  //constructor for countbomb tests
  Minesweeper(Random rand, int w, int h, int mine, boolean populate) {
    this.cellsRevealed = 0;
    this.rand = rand;
    this.initCells(w, h, mine);
    if (populate) {
      this.populateNeighbors(w, h);
    }
    this.countMines();
  }

  //constructor for game
  Minesweeper() {
    this(new Random(), true);
  }

  //initializes cells and adds mines
  void initCells(int numColumns, int numRows, int numMines) {
    ArrayList<ArrayList<Cell>> rows = new ArrayList<ArrayList<Cell>>();
    ArrayList<Cell> column;
    //generate rows
    for (int countRows = 0; countRows < numRows; countRows++) {
      //generate columns
      column = new MineUtils().createColumns(numColumns);
      rows.add(column);
    }
    //add mines
    this.rowsOfCells = rows;
    this.addMines(numMines, numColumns, numRows);
  }

  //adds mines to cells
  void addMines(int numMines, int numColumns, int numRows) {
    ArrayList<Integer> mineIndices = 
        new MineUtils().generateIndices(numMines, this.rand, numColumns * numRows);
    for (int i = 0; i < numMines; i++) {
      //create indices
      int index = mineIndices.get(i);
      int rowIndex = index % numRows;
      int columnIndex = index / numRows;

      //get and modify cell
      ArrayList<Cell> column = this.rowsOfCells.get(rowIndex);
      Cell mineCell = column.get(columnIndex);
      mineCell.mine = true;

      //add cell back to cells
      column.set(columnIndex, mineCell);
      this.rowsOfCells.set(rowIndex, column);
    }
  }

  //takes in a table of cells and updates their neighbors
  void populateNeighbors(int numRows, int numColumns) {
    for (int row = 0; row < this.rowsOfCells.size(); row++) {
      ArrayList<Cell> aRow = this.rowsOfCells.get(row);
      for (int column = 0; column < aRow.size(); column++) {
        Cell cell = aRow.get(column);
        //update neighbors
        this.updateNeighbors(cell, row, column, numRows, numColumns);
        //update cell
        aRow.set(column, cell);
      }
      this.rowsOfCells.set(row, aRow);
    }
  }

  //finds and adds all the neighbors of the given cell
  void updateNeighbors(Cell cell, int row, int column, int numRows, 
      int numColumns) {
    ArrayList<Cell> neighbors = cell.neighbors;
    //if there exists a row beneath
    if (row + 1 < numRows) {

      //if there exists a column after
      if (column + 1 < numColumns) {
        //add unique bottomright cell to neighbors
        Cell bottomRight = this.rowsOfCells.get(row + 1).get(column + 1);
        if (!neighbors.contains(bottomRight)) {
          neighbors.add(bottomRight);
        }
      }

      //if there exists a column before
      if (column - 1 >= 0) {
        //add unique bottom right cell to neighbors
        Cell bottomLeft = this.rowsOfCells.get(row + 1).get(column - 1);
        if (!neighbors.contains(bottomLeft)) {
          neighbors.add(bottomLeft);
        }
      }

      //add bottom middle cell 
      neighbors.add(this.rowsOfCells.get(row + 1).get(column));
    }

    //if there exists a row above
    if (row - 1 >= 0) {

      //if there exists a column after
      if (column + 1 < numColumns) {
        Cell topRight = this.rowsOfCells.get(row - 1).get(column + 1);
        if (!neighbors.contains(topRight)) {
          neighbors.add(topRight);
        }
      }

      //if there exists a column before
      if (column - 1 >= 0) {
        Cell topLeft = this.rowsOfCells.get(row - 1).get(column - 1);
        if (!neighbors.contains(topLeft)) {
          neighbors.add(topLeft);
        }
      }

      //add top middle cell
      neighbors.add(this.rowsOfCells.get(row - 1).get(column));
    }

    //if there exists a column after
    if (column + 1 < numColumns) {
      neighbors.add(this.rowsOfCells.get(row).get(column + 1));
    }

    //if there exists a column before
    if (column - 1 >= 0) {
      neighbors.add(this.rowsOfCells.get(row).get(column - 1));
    }
  }

  //counts the number of mines surrounding this cell and updates the value accordingly
  void countMines() {
    ArrayList<ArrayList<Cell>> rows = this.rowsOfCells;
    for (ArrayList<Cell> row : rows) {
      for (Cell cell : row) {
        cell.value = new MineUtils().countMines(cell.neighbors);
      }
    }
  }

  //draws the minesweeper game
  public WorldScene makeScene() {
    int middleX = SCREEN_WIDTH / 2;
    int middleY = SCREEN_HEIGHT / 2;
    WorldScene scene = EMPTY_SCENE;

    scene.placeImageXY(new RectangleImage(CELL_SIZE * GRID_WIDTH, CELL_SIZE * GRID_HEIGHT, 
        OutlineMode.SOLID, CELL_UCOLOR), middleX, middleY);
    scene.placeImageXY(this.drawCells(), middleX, middleY);
    return scene;
  }

  //draws all cells
  WorldImage drawCells() {
    WorldImage grid = new EmptyImage();
    for (ArrayList<Cell> row : this.rowsOfCells) {
      WorldImage rowImage = new EmptyImage();
      for (Cell cell : row) {
        WorldImage cellImage = cell.drawCell();
        rowImage = new BesideImage(rowImage, cellImage);
      }
      grid = new AboveImage(grid, rowImage);
    }
    return grid;
  }

  //when mouse is clicked on a cell, reveals it and handles mines
  public void onMouseClicked(Posn pos, String buttonName) {
    int rowNum = pos.y / CELL_SIZE;
    int colNum = pos.x / CELL_SIZE;

    //get cell that was clicked
    ArrayList<Cell> row = this.rowsOfCells.get(rowNum);
    Cell cell = row.get(colNum);

    if (buttonName.equals("LeftButton")) {
      //if mine end the game
      if (cell.mine) {
        this.endOfWorld("lose");
      }
      // reveal the cell
      this.cellsRevealed += cell.reveal(new ArrayList<Cell>());

      //check win condition
      if (this.cellsRevealed >= (GRID_WIDTH * GRID_HEIGHT) - NUM_MINES) {
        this.endOfWorld("win");
      }
    }
    if (buttonName.equals("RightButton")) {
      cell.flagged = !cell.flagged;
    }
  }

  //returns the game-over scene
  public WorldScene lastScene(String msg) {
    WorldScene empty = this.getEmptyScene();
    WorldImage loseText = new TextImage("Haha you lost", 50, Color.RED);
    WorldImage winText = new TextImage("You finally won!", 50, Color.GREEN);
    WorldImage text;
    if (msg.equals("lose")) {
      text = loseText;
    }
    else {
      text = winText;
    }
    empty.placeImageXY(text, SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2);
    return empty;
  }
}

//represents a cell in a minesweeper game
class Cell {
  boolean revealed;
  ArrayList<Cell> neighbors;
  boolean mine;
  int value;
  boolean flagged;

  //default constructor
  Cell(boolean revealed, boolean mine, ArrayList<Cell> neighbors, int value, boolean flagged) {
    this.revealed = revealed;
    this.mine = mine;
    this.neighbors = neighbors;
    this.value = value;
    this.flagged = flagged;
  }

  //convenience constructors
  Cell() {
    this(false, false, new ArrayList<Cell>(), 0, false);
  }

  Cell(boolean revealed, boolean mine, ArrayList<Cell> neighbors, int value) {
    this(revealed, mine, neighbors, value, false);
  }

  //test constructor
  Cell(boolean revealed, boolean mine) {
    this(revealed, mine, new ArrayList<Cell>(), 0, false);
  }

  //EFFECT: reveals a cell, if no adjacent mines, activates floodFill
  //returns number of cells revealed
  public int reveal(ArrayList<Cell> revealedCells) {
    if (!revealedCells.contains(this)) {
      this.revealed = true;
      revealedCells.add(this);
      if (this.value == 0) {
        this.floodFill(revealedCells);
      }
    }
    return revealedCells.size();
  }

  //EFFECT: reveals cells adjacent to cell recursively 
  void floodFill(ArrayList<Cell> revealedCells) {
    for (Cell cell : this.neighbors) {
      cell.reveal(revealedCells);
    }
  }

  //draws a singular cell
  WorldImage drawCell() {
    int cellSize = Minesweeper.CELL_SIZE;
    WorldImage unrevealedCell = new RectangleImage(cellSize, cellSize, 
        OutlineMode.OUTLINE, Color.BLACK);

    //if revealed
    if (this.revealed) {
      WorldImage revealedCell = new RectangleImage(cellSize, cellSize, 
          OutlineMode.SOLID, Minesweeper.CELL_RCOLOR);

      //if mine draw a red circle on top of a revealed cell
      if (this.mine) {
        return new OverlayImage(new CircleImage(cellSize / 5, OutlineMode.SOLID, Color.RED),
            revealedCell);
      }
      //otherwise draw the value of the cell on top of a revealed cell
      return new OverlayImage(new TextImage("" + this.value, cellSize * 0.75, 
          Minesweeper.VALUE_COLORS.get(this.value)),
          revealedCell);
    }

    //if flagged draw an orange circle on top of the cell
    if (this.flagged) {
      return new OverlayImage(new CircleImage(cellSize / 5, OutlineMode.SOLID, Color.orange), 
          unrevealedCell);
    }

    //if all conditions are false, draw an unrevealed cell
    return unrevealedCell;
  }
}

//misc stuff
class MineUtils {

  //creates list of cells with given size
  ArrayList<Cell> createColumns(int numColumns) {
    ArrayList<Cell> column = new ArrayList<Cell>();
    for (int count = 0; count < numColumns; count++) {
      column.add(new Cell());
    }
    return column;
  }

  //returns the number of mines in a given arraylist of cells
  int countMines(ArrayList<Cell> neighbors) {
    int count = 0;
    //for each cell that is a mine, add 1 to count
    for (Cell cell : neighbors) {
      if (cell.mine) {
        count += 1;
      }
    }
    return count;
  }

  //creates an arraylist of indices given the number to create and the upper bound
  ArrayList<Integer> generateIndices(int numMines, Random rand, int upperBound) {
    ArrayList<Integer> indices = new ArrayList<Integer>();
    //pick random numbers from sample space and add to LoIndices
    while (indices.size() < numMines) {
      //generate random number
      int randIndex = rand.nextInt(upperBound);
      //check if it is already in the list
      if (!indices.contains(randIndex)) {
        //if not add to list
        indices.add(randIndex);
      }
    }
    return indices;
  }
}

class ExamplesMinesweeper {
  Minesweeper game1;
  Minesweeper game;
  Minesweeper game3;
  Random testRand;
  ArrayList<ArrayList<Cell>> empty;
  ArrayList<ArrayList<Cell>> test4x4;
  ArrayList<Cell> empty2;
  ArrayList<Cell> fourNoMine;
  ArrayList<Cell> four1Mine;
  MineUtils util = new MineUtils();
  Cell noMine;
  Cell mine;

  ExamplesMinesweeper() {
    init();
  }

  //resets test values
  void init() {
    empty = new ArrayList<ArrayList<Cell>>();
    empty2 = new ArrayList<Cell>();
    testRand = new Random(33);
    game1 = new Minesweeper(testRand, 25, 25, 100, false);
    game3 = new Minesweeper(testRand, 4, 4, 0, true);
    noMine = new Cell();
    mine = new Cell(false, true);
    game = new Minesweeper(testRand, 3, 3, 1, true);

    fourNoMine = new ArrayList<Cell>();
    fourNoMine.add(noMine);
    fourNoMine.add(noMine);
    fourNoMine.add(noMine);
    fourNoMine.add(noMine);

    four1Mine = new ArrayList<Cell>();
    four1Mine.add(noMine);
    four1Mine.add(noMine);
    four1Mine.add(noMine);
    four1Mine.add(mine);

    test4x4 = new ArrayList<ArrayList<Cell>>();
    test4x4.add(fourNoMine);
    test4x4.add(fourNoMine);
    test4x4.add(four1Mine);
    test4x4.add(fourNoMine);
  }


  void testinitCells(Tester t) {
    init();
    game1.initCells(4, 4, 1);
    t.checkExpect(game1.rowsOfCells, test4x4);
    init();
    game1.initCells(0, 0, 0);
    t.checkExpect(game1.rowsOfCells, empty);
    init();
    ArrayList<Cell> oneMine = new ArrayList<Cell>();
    oneMine.add(mine);
    empty.add(oneMine);
    game1.initCells(1, 1, 1);
    t.checkExpect(game1.rowsOfCells, empty);
  }

  void testgenerateIndices(Tester t) {
    init();
    t.checkExpect(util.generateIndices(0, testRand, 0), new ArrayList<Integer>());
    ArrayList<Integer> oneToFive = new ArrayList<Integer>(Arrays.asList(1, 4, 0, 2, 3));
    t.checkExpect(util.generateIndices(5, testRand, 5), oneToFive);
  }

  void testcreateColumns(Tester t) {
    init(); 
    t.checkExpect(util.createColumns(0), new ArrayList<Cell>());
    t.checkExpect(util.createColumns(4), fourNoMine);
  }

  void testupdateNeighbors(Tester t) {
    init();
    game1.rowsOfCells = test4x4;
    game1.updateNeighbors(noMine, 0, 0, 4, 4);
    ArrayList<Cell> threeCell = new ArrayList<Cell>();
    Cell testCell = new Cell(false, false, threeCell, 0);
    threeCell.add(testCell); 
    threeCell.add(testCell); 
    threeCell.add(testCell);
    t.checkExpect(test4x4.get(0).get(0).neighbors, threeCell);
  }

  void testcountMinesinMinesweeper(Tester t) {
    init();
    game.countMines();
    t.checkExpect(game.rowsOfCells.get(1).get(1).neighbors.size(), 8);
    t.checkExpect(game.rowsOfCells.get(0).get(0).value, 1);
    t.checkExpect(game.rowsOfCells.get(0).get(1).value, 1);
    t.checkExpect(game.rowsOfCells.get(0).get(2).value, 1);
    t.checkExpect(game.rowsOfCells.get(1).get(0).value, 1);
    t.checkExpect(game.rowsOfCells.get(1).get(1).value, 0);
    t.checkExpect(game.rowsOfCells.get(1).get(2).value, 1);
    t.checkExpect(game.rowsOfCells.get(2).get(0).value, 1);
    t.checkExpect(game.rowsOfCells.get(2).get(1).value, 1);
    t.checkExpect(game.rowsOfCells.get(2).get(2).value, 1);
  }

  void testcountMinesinUtils(Tester t) {
    init();
    t.checkExpect(util.countMines(this.four1Mine), 1);
    t.checkExpect(util.countMines(this.fourNoMine), 0);
  }

  void testReveal(Tester t) {
    init();
    t.checkExpect(game1.rowsOfCells.get(0).get(0).mine, false);
    t.checkExpect(game1.rowsOfCells.get(0).get(0).reveal(empty2), 1);
    t.checkExpect(game1.rowsOfCells.get(9).get(8).reveal(empty2), 2);
    init();
    t.checkExpect(game3.rowsOfCells.get(0).get(0).reveal(empty2), 16);
    init();
    t.checkExpect(new Cell().reveal(empty2), 1);
    init();
    t.checkExpect(noMine.reveal(new ArrayList<Cell>(Arrays.asList(noMine))), 1);
  }

  //test floodfill
  void testFloodFill(Tester t) {
    init();
    game3.rowsOfCells.get(0).get(0).floodFill(empty2);
    for (ArrayList<Cell> row : game3.rowsOfCells) {
      for (Cell cell : row) {
        t.checkExpect(cell.revealed, true);
      }
    }
  }

  void testDrawCell(Tester t) {
    init();
    WorldImage revealedImage = new RectangleImage(Minesweeper.CELL_SIZE,
        Minesweeper.CELL_SIZE, OutlineMode.SOLID, Color.DARK_GRAY);
    WorldImage unrevealedImage = new RectangleImage(Minesweeper.CELL_SIZE, 
        Minesweeper.CELL_SIZE, OutlineMode.OUTLINE, Color.BLACK);
    Cell revealedMine = new Cell(true, true);
    Cell revealed = new Cell(true, false);
    Cell unrevealedFlag = new Cell(false, false, empty2, 0, true);

    t.checkExpect(noMine.drawCell(), unrevealedImage);
    t.checkExpect(revealed.drawCell(), new OverlayImage(
        new TextImage("0", 37.5, FontStyle.REGULAR, Color.DARK_GRAY), 
        revealedImage));
    t.checkExpect(revealedMine.drawCell(), new OverlayImage(
        new CircleImage(10, OutlineMode.SOLID, Color.RED), revealedImage));
    t.checkExpect(unrevealedFlag.drawCell(), new OverlayImage(
        new CircleImage(10, OutlineMode.SOLID, Color.ORANGE), unrevealedImage));
  }

  void testDrawCells(Tester t) {
    WorldImage unrevealed = new RectangleImage(50, 50, OutlineMode.OUTLINE, Color.BLACK);
    init();
    Minesweeper game4 = new Minesweeper(testRand, 1, 1, 0, false);
    t.checkExpect(game4.drawCells(), new AboveImage(
        new EmptyImage(), new BesideImage(new EmptyImage(), unrevealed)));
    Minesweeper game5 = new Minesweeper(testRand, 2, 2, 0, false);
    t.checkExpect(game5.drawCells(), 
        new AboveImage(new AboveImage(new EmptyImage(),
            new BesideImage(new BesideImage(new EmptyImage(), unrevealed), unrevealed)),
            new BesideImage(new BesideImage(new EmptyImage(), unrevealed), unrevealed)));
  }

  void testPopulateNeighbors(Tester t) {
    init();
    Minesweeper game = new Minesweeper(testRand, 3, 3, 0, false);
    t.checkExpect(game.rowsOfCells.get(1).get(1).neighbors.size(), 0);
    game.populateNeighbors(3, 3);
    t.checkExpect(game.rowsOfCells.get(1).get(1).neighbors.size(), 8);
  }

  void testmakeScene(Tester t) {
    WorldImage unrevealed = new RectangleImage(50, 50, OutlineMode.OUTLINE, Color.BLACK);
    WorldImage image = new AboveImage(
        new EmptyImage(), new BesideImage(new EmptyImage(), unrevealed));
    WorldScene empty = new WorldScene(1650, 800);
    empty.placeImageXY(new RectangleImage(1650, 800, OutlineMode.SOLID, Color.GRAY), 825, 400);
    empty.placeImageXY(image, 825, 400);
    init();
    Minesweeper game = new Minesweeper(testRand, 1, 1, 0, false);
    t.checkExpect(game.makeScene(), empty);
  }

  //also tests game end
  void testonMouseClick(Tester t) {
    init();
    game.onMouseClicked(new Posn(40, 40), "LeftButton");
    t.checkExpect(game.cellsRevealed, 1);
    game.onMouseClicked(new Posn(40, 90), "LeftButton");
    t.checkExpect(game.cellsRevealed, 2);
    game.onMouseClicked(new Posn(40, 140), "LeftButton");
    t.checkExpect(game.cellsRevealed, 3);
    game.onMouseClicked(new Posn(90, 40), "LeftButton");
    t.checkExpect(game.cellsRevealed, 4);
    //click on mine
    game.onMouseClicked(new Posn(90, 90), "LeftButton");
    t.checkExpect(game.cellsRevealed, 13);
    game.onMouseClicked(new Posn(40, 40), "RightButton");
    t.checkExpect(game.rowsOfCells.get(0).get(0).flagged, true);
    game.onMouseClicked(new Posn(40, 40), "RightButton");
    t.checkExpect(game.rowsOfCells.get(0).get(0).flagged, false);
  }

  //  void testGame(Tester t) {
  //    Minesweeper g = new Minesweeper();
  //    g.bigBang(Minesweeper.SCREEN_WIDTH, Minesweeper.SCREEN_HEIGHT);
  //  }
}