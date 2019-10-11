//IGNORE COMMENTED OUT CODE, IT IS FOR PART 2
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import tester.*;
import javalib.impworld.*;

import java.awt.Color;
import javalib.worldimages.*;

class LightEmAll extends World {
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int width;
  int height;
  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;
  Random rand;
  int time;
  int moves;
  //dictionary of colors for powerlines
  HashMap<Integer, Color> lineColors = new HashMap<Integer, Color>();
  //indicator for end-game
  boolean gameWon;

  //CONSTANTS
  static final int CELL_SIZE = 50;
  static final int LINE_WEIGHT = CELL_SIZE / 15;
  static final int LINE_LENGTH = CELL_SIZE / 2;
  //powerstation image
  public final static WorldImage PW_STATION = new OverlayImage(
      new StarImage(20,  7, OutlineMode.OUTLINE, Color.YELLOW),
      new StarImage(20, 7, OutlineMode.SOLID, Color.CYAN));
  //dictionary of offsets for line directions when drawing lines
  public final static HashMap<String, ArrayList<Integer>> LINE_OFFSETS = 
      new HashMap<String, ArrayList<Integer>>();

  //initialize hashmap
  static {
    int cellSize = CELL_SIZE;
    int lineWt = LINE_WEIGHT;
    int lineLn = LINE_LENGTH;
    LINE_OFFSETS.put("top", 
        new ArrayList<Integer>(Arrays.asList(lineWt, lineLn, 0, cellSize / 4)));
    LINE_OFFSETS.put("bottom", 
        new ArrayList<Integer>(Arrays.asList(lineWt, lineLn, 0, -(cellSize / 4))));
    LINE_OFFSETS.put("left", 
        new ArrayList<Integer>(Arrays.asList(lineLn, lineWt, cellSize / 4, 0)));
    LINE_OFFSETS.put("right", 
        new ArrayList<Integer>(Arrays.asList(lineLn, lineWt, -(cellSize / 4), 0)));
  }

  //default constructor
  LightEmAll(ArrayList<ArrayList<GamePiece>> board, ArrayList<GamePiece> nodes, 
      ArrayList<Edge> mst, int width, int height, int powerRow, 
      int powerCol, int radius, Random rand) {
    this.board = board;
    this.nodes = nodes;
    this.mst = mst;
    this.width = width;
    this.height = height;
    this.powerRow = powerRow;
    this.powerCol = powerCol;
    this.radius = radius;
    this.rand = rand;
  }

  //constructor for parts 1 and 2
  LightEmAll(int width, int height, boolean fractal) {
    this.width = width;
    this.height = height;
    this.time = 0;
    if (fractal) {
      this.initBoardFractal(width, height);
      this.placePowerStation((int) Math.ceil((width - 1) / 2.0), 0);
    }
    else {
      this.initBoard(width, height);
      this.placePowerStation(0, 0);
    }
    this.connectNeighbors();
    this.radius = this.computeRadius();
  }

  //constructor for Part 3
  LightEmAll(int width, int height, Random rand) {
    this.width = width;
    this.height = height;
    this.rand = rand;
    this.time = 0;

    this.initBoardKruskal(width, height);
    this.placePowerStation(0, 0);
    this.connectNeighbors();
    this.radius = this.computeRadius();
    this.randomizePieces();
    this.connectNeighbors();
    if (radius > 0) {
      this.genColors();
    }
  }


  //initializes the board given the dimensions
  //for Part 1
  //all vertical ines with one horizontal line in the middle
  //EFFECT: initializes board and nodes
  void initBoard(int width, int height) {
    ArrayList<ArrayList<GamePiece>> builtList = new ArrayList<ArrayList<GamePiece>>();
    ArrayList<GamePiece> flatList = new ArrayList<GamePiece>();
    for (int colNum = 0; colNum < width; colNum++) {
      ArrayList<GamePiece> row = new ArrayList<GamePiece>();
      for (int rowNum = 0; rowNum < height; rowNum++) {
        GamePiece currentCell = new GamePiece(colNum, rowNum);
        if (rowNum != 0) { //not in top row
          currentCell.top = true;
        }
        if (rowNum != height - 1) { // not in bottom row
          currentCell.bottom = true;
        }
        if (rowNum == height / 2) { // in the middle
          if (colNum != 0) { // not leftmost
            currentCell.left = true;
          }
          if (colNum != width - 1) { // not rightmost
            currentCell.right = true;
          }
        }
        row.add(currentCell);
        flatList.add(currentCell);
      }
      builtList.add(row);
    }
    this.board = builtList;
    this.nodes = flatList;
  }

  //creates the board for part 2
  //uses a sub-division algorithm to create a fractal
  //initializes board and nodes, and places powerstation
  void initBoardFractal(int width, int height) {
    ArrayList<ArrayList<GamePiece>> builtList = new ArrayList<ArrayList<GamePiece>>();
    ArrayList<GamePiece> flatList = new ArrayList<GamePiece>();
    for (int colNum = 0; colNum < width; colNum++) {
      ArrayList<GamePiece> row = new ArrayList<GamePiece>();
      for (int rowNum = 0; rowNum < height; rowNum++) {
        GamePiece newCell = new GamePiece(false, false, false, false, false, colNum, rowNum);
        row.add(newCell); //add to row for this.board
        flatList.add(newCell); //add to flatList for this.nodes
      }
      builtList.add(row);
    }
    this.board = builtList;
    this.nodes = flatList;
    this.fractcalHelper(this.nodes, this.height, this.width); //generate fractal pattern
  }

  //handles base cases (2x2, 1xX, 2xX) else continues to subdivide
  //only called with non-empty arraylists and their heights and widths
  void fractcalHelper(ArrayList<GamePiece> subdivision, int subdivHeight, int subdivWidth) {
    int subSize = subdivision.size();
    if (subSize == 1) { //if single cell (will only happen in bottom R of subdivision)
      subdivision.get(0).left = true;
      subdivision.get(0).top = true;
    }
    else if (subdivHeight == 1) { //if height is one generate straight line
      for (GamePiece piece : subdivision) {
        int index = subdivision.indexOf(piece);
        if (index == 0) {
          piece.right = true;
        }
        else if (index == subdivision.size() - 1) {
          piece.left = true;
        }
        else {
          piece.left = true;
          piece.right = true;
        }
      }
    }
    else if (subdivHeight == 2 && subdivWidth != 1) { //if height is 2 but not just a single cell)
      for (GamePiece piece : subdivision) {           //generate single-line fractal
        int index = subdivision.indexOf(piece);
        if (index % 2 == 0) {
          piece.bottom = true;
        }
        else {
          piece.top = true;
        }
        if (index == 1) {
          piece.right = true;
        }
        else if (index == subdivision.size() - 1) {
          piece.left = true;
        }
        else if (index % 2 == 1) {
          piece.left = true;
          piece.right = true;
        }
      }
    }
    else if (subdivWidth <= 2) { // if width is one, generate straight line (vertical)
      for (GamePiece piece : subdivision) {
        int index = subdivision.indexOf(piece);
        piece.right = false;
        if (index == 0) {
          piece.bottom = true;
        }
        else if (index == subSize - 1) {
          if (subdivWidth == 2) {
            piece.left = true;
          }
          piece.top = true;
        }
        else if (index == subdivHeight - 1 && subdivWidth == 2) {
          piece.right = true;
          piece.top = true;
        }
        else {
          piece.bottom = true;
          piece.top = true;
        }
        if (index == subdivHeight && subdivWidth == 2) {
          piece.top = false;
        }
      }
    }
    else { //otherwise continue to divide the board
      this.subdivide(subdivision, subdivHeight, subdivWidth);
    }
  }

  //divides the board in quadrants (or as close as possible) and calls fractalHelper,
  //connecting the resulting quadrants together
  void subdivide(ArrayList<GamePiece> orig, int origH, int origW) {
    ArrayList<GamePiece> topL = new ArrayList<>();
    ArrayList<GamePiece> topR = new ArrayList<>();
    ArrayList<GamePiece> bottomL = new ArrayList<>();
    ArrayList<GamePiece> bottomR = new ArrayList<>();

    //width and height of first quadrant
    int cutoffH = (int) Math.ceil(origH / 2.0);
    int cutoffW = (int) Math.ceil(origW / 2.0);

    //get gamepiece
    for (int col = 0; col < origW; col++) {
      for (int row = 0; row < origH; row++) {
        GamePiece current = orig.get((col * origH) + row);

        //sort into quadrant
        if (row < cutoffH) {
          if (col < cutoffW) {
            topL.add(current);
          }
          else {
            topR.add(current);
          }
        }
        else {
          if (col < cutoffW) {
            bottomL.add(current);
          }
          else {
            bottomR.add(current);
          }
        }
      }
    }

    //check for base cases
    this.fractcalHelper(topL, cutoffH, cutoffW);
    this.fractcalHelper(topR, cutoffH, origW - cutoffW);
    this.fractcalHelper(bottomL, origH - cutoffH, cutoffW);
    this.fractcalHelper(bottomR, origH - cutoffH, origW - cutoffW);

    //connect quadrants together
    topL.get(cutoffH - 1).bottom = true;
    topR.get(topR.size() - 1).bottom = true;
    bottomL.get(bottomL.size() - 1).right = true;
    bottomL.get(0).top = true;
    bottomR.get(origH - cutoffH - 1).left = true;
    bottomR.get(bottomR.size() - (origH - cutoffH)).top = true;
  }

  //initializes board for Part 3
  //uses Kruskals algorithm: connects all GP together, 
  //assigns edge weights on random, and creates an MST
  void initBoardKruskal(int width, int height) {

    //connect all GP together
    ArrayList<ArrayList<GamePiece>> builtList = new ArrayList<ArrayList<GamePiece>>();
    ArrayList<GamePiece> flatList = new ArrayList<GamePiece>();
    for (int colNum = 0; colNum < width; colNum++) {
      ArrayList<GamePiece> row = new ArrayList<GamePiece>();
      for (int rowNum = 0; rowNum < height; rowNum++) {
        GamePiece currentCell = new GamePiece(colNum, rowNum);
        if (rowNum != 0) { //not in top row
          currentCell.top = true;
        }
        if (rowNum != height - 1) { // not in bottom row
          currentCell.bottom = true;
        }
        if (colNum != 0) { // not in first column
          currentCell.left = true;
        }
        if (colNum != width - 1) { //not in last column
          currentCell.right = true;
        }
        row.add(currentCell);
        flatList.add(currentCell);
      }
      builtList.add(row);
    }
    this.board = builtList;
    this.nodes = flatList;

    //connect neighbors
    this.connectNeighbors();
    //build a list of edges
    ArrayList<Edge> allEdges = this.createEdges();
    //sort the edges and create a minimum spanning tree
    this.mst = this.kruskalsort(allEdges);
    //clear the board
    this.clearWires();
    //draw the spanning tree
    this.drawMst();
  }

  //uses Kruskals algorithm to return a list of edges with a minimum spanning tree
  //takes in unsorted worklist
  ArrayList<Edge> kruskalsort(ArrayList<Edge> worklist) {
    HashMap<GamePiece, GamePiece> reps = new HashMap<GamePiece, GamePiece>();
    worklist.sort(new SortByWeight());
    ArrayList<Edge> mst = new ArrayList<Edge>();

    //initialize each nodes representative to itself
    for (GamePiece node : this.nodes) {
      reps.put(node, node);
    }

    while (!worklist.isEmpty()) {
      Edge current = worklist.remove(0);

      //if the representatives aren't equal
      GamePiece from = find(reps, current.fromNode);
      GamePiece to = find(reps, current.toNode);
      if (!from.sameGP(to)) {
        mst.add(current);
        union(reps, from, to);
      }
    }
    return mst;
  }

  //clears the wires on the board
  void clearWires() {
    for (GamePiece node : this.nodes) {
      node.bottom = false;
      node.top = false;
      node.left = false;
      node.right = false;
    }
  }

  //draws wires based on mst
  void drawMst() {
    for (Edge edge : this.mst) {
      edge.fromNode.connectTo(edge.toNode);
    }
  }

  //finds the representative of the given GamePiece in the given map
  GamePiece find(HashMap<GamePiece, GamePiece> reps, GamePiece node) {
    if (reps.get(node).equals(node)) {
      return node;
    }
    else {
      return find(reps, reps.get(node));
    }
  }

  //sets the value of the to representative to the from representative
  void union(HashMap<GamePiece, GamePiece> reps, GamePiece fromRep, GamePiece toRep) {
    reps.replace(toRep, fromRep);
  }

  //creates edges with random weights
  ArrayList<Edge> createEdges() {
    ArrayList<Edge> edges = new ArrayList<Edge>();
    ArrayList<GamePiece> visited = new ArrayList<GamePiece>();
    for (GamePiece node : this.nodes) {
      visited.add(node);
      for (GamePiece neighbor : node.neighbors) {
        if (!visited.contains(neighbor)) {
          int weight = this.rand.nextInt();
          edges.add(new Edge(node, neighbor, weight));
        }
      }
    }
    return edges;
  }

  //connects the neighbors of gamepieces together
  void connectNeighbors() {
    for (int colNum = 0; colNum < width; colNum++) {
      for (int rowNum = 0; rowNum < height; rowNum++) {
        GamePiece currentCell = this.board.get(colNum).get(rowNum);
        currentCell.neighbors = new ArrayList<GamePiece>();
        if (rowNum != height - 1 && currentCell.bottom // if not in bottom row
            && this.board.get(colNum).get(rowNum + 1).top) { //and both cells point to ea/other
          currentCell.neighbors.add(this.board.get(colNum).get(rowNum + 1));
        }
        if (rowNum != 0 && currentCell.top // if not in top row
            && this.board.get(colNum).get(rowNum - 1).bottom) { //and both cells point to ea/other
          currentCell.neighbors.add(this.board.get(colNum).get(rowNum - 1));
        }
        if (colNum != 0 && currentCell.left // if not in left column
            && this.board.get(colNum - 1).get(rowNum).right) { // and both cells point to ea/other
          currentCell.neighbors.add(this.board.get(colNum - 1).get(rowNum));
        }
        if (colNum != width - 1 && currentCell.right // if not in right column
            && this.board.get(colNum + 1).get(rowNum).left) { // and both cells point to ea/other
          currentCell.neighbors.add(this.board.get(colNum + 1).get(rowNum));
        }
      }
    }
  }

  //EFFECT: places the powerstation onto the board
  //for Part 1 only places it on the top-left GamePiece
  void placePowerStation(int col, int row) {
    this.board.get(col).get(row).powerStation = true;
    this.powerCol = col;
    this.powerRow = row;
  }

  //updates time
  public void onTick() {

    //check win condition
    this.gameWon = true;
    for (GamePiece node : this.nodes) {
      int distFromPWS = this.distFrom(node, 
          this.board.get(powerCol).get(powerRow), new ArrayList<GamePiece>());
      this.gameWon = distFromPWS <= this.radius  //distance from powerstation is less than the rad
          && distFromPWS != -1  // and a path exists between cell and radius
          && this.gameWon; // and every other node is true;
    }

    if (!gameWon) {
      this.time += 1;
    }
  }

  //handles mouse click behavior
  public void onMouseClicked(Posn p) {
    if (!gameWon && this.inBounds(p)) { // game not won yet and click is in bounds
      this.moves += this.board.get((p.x - 50) / CELL_SIZE).get(p.y / CELL_SIZE).turnClockwise();
      this.connectNeighbors();
    }
    else if (gameWon) { //reinitalize game
      this.initBoardKruskal(width, height);
      this.placePowerStation(0, 0);
      this.connectNeighbors();
      this.radius = this.computeRadius();
      this.randomizePieces();
      this.connectNeighbors();
      this.time = 0;
      this.moves = 0;
      this.gameWon = false;
      if (radius > 0) {
        this.genColors();
      }
    }
  }

  //key press handler (moves powerstation)
  //also checks for Win conditions
  public void onKeyEvent(String key) {
    GamePiece old = this.board.get(powerCol).get(powerRow);
    GamePiece newGP;

    //handle moving of powerstation
    if ((key.equals("up") || key.equals("w")) && this.powerRow != 0) {
      newGP = this.board.get(powerCol).get(powerRow - 1);
      if (old.neighbors.contains(newGP)) {
        old.powerStation = false;
        this.powerRow += -1;
        newGP.powerStation = true;
      }
    }
    if ((key.equals("down") || key.equals("s")) && this.powerRow != this.height - 1) {
      newGP = this.board.get(powerCol).get(powerRow + 1);
      if (old.neighbors.contains(newGP)) {
        old.powerStation = false;
        this.powerRow += 1;
        newGP.powerStation = true;
      }
    }
    if ((key.equals("left") || key.equals("a")) && this.powerCol != 0) {
      newGP = this.board.get(powerCol - 1).get(powerRow);
      if (old.neighbors.contains(newGP)) {
        old.powerStation = false;
        this.powerCol += -1;
        newGP.powerStation = true;
      }
    }
    if ((key.equals("right") || key.equals("d")) && this.powerCol != this.width - 1) {
      newGP = this.board.get(powerCol + 1).get(powerRow);
      if (old.neighbors.contains(newGP)) {
        old.powerStation = false;
        this.powerCol += 1;
        newGP.powerStation = true;
      }
    }
  }

  //generates the line colors for this board
  //requires radius to be set
  public void genColors() {
    int rInc = (255 - 96) / this.radius;
    int gInc = (255 - 63) / this.radius;
    int bInc = 31 / this.radius;
    for (int i = 0; i <= this.radius; i++) {
      Color color = new Color(255 - (rInc * i), 255 - (gInc * i), 0 + (bInc * i));
      this.lineColors.put(i, color);
    }
  }

  //checks if the position is in the bounds of the game
  public boolean inBounds(Posn p) {
    return (p.x <= this.width * CELL_SIZE + 50) && (p.x >= 50)
        && (p.y <= this.height * CELL_SIZE) && (p.y >= 0);
  }

  //draws all of the gamepieces
  public WorldImage drawGamePieces() {
    Color color;
    WorldImage boardImage = new EmptyImage();

    //get gamepiece
    for (ArrayList<GamePiece> column : this.board) {
      WorldImage columnImage = new EmptyImage();
      for (GamePiece cell : column) {

        //get distance from powerstation
        int distFromPWS = this.distFrom(cell, this.board.get(powerCol).get(powerRow),
            new ArrayList<GamePiece>());
        color = lineColors.getOrDefault(distFromPWS, Color.LIGHT_GRAY);
        columnImage = new AboveImage(columnImage, cell.draw(color));
      }
      boardImage = new BesideImage(boardImage, columnImage);
    }
    return boardImage;
  }

  //calculates the distance from one gamepiece to pwstation
  //returns -1 if no path between two
  public int distFrom(GamePiece from, GamePiece to, ArrayList<GamePiece> visited) {
    int path = -1;
    if (from.col == to.col && from.row == to.row) {
      return 0; //if path found, return 0
    }
    else if (from.neighbors.size() == 0) {
      return -1; //if no neighbors, no path, so return -1
    }
    else {
      visited.add(from);

      //for each neighbor
      for (GamePiece neighbor : from.neighbors) {
        if (!visited.contains(neighbor)) { //if not already visited
          path = this.distFrom(neighbor, to, visited); //find path from neighbor to given dest
          if (path != -1) { //if a path exists
            path += 1; //add depth
            break; //stop loop
          }
        }
      }
      return path;
    }
  }

  //renders the big-bang scene
  public WorldScene makeScene() {
    int screenWidth = this.width * CELL_SIZE + 100;
    int screenHeight = this.height * CELL_SIZE;
    //empty scene
    WorldScene scene = new WorldScene(screenWidth, screenHeight);
    //shows time
    WorldImage timeBox = new AboveImage(new TextImage("TIME", 20, Color.RED), 
        new TextImage("" + this.time, 20, Color.RED));
    //shows number of moves
    WorldImage moveBox = new AboveImage(new TextImage("MOVES", 14, Color.RED), 
        new TextImage("" + this.moves, 20, Color.RED));
    //game won text
    WorldImage gameWonText = new TextImage("You Won!", 20, Color.GREEN);
    //help text
    WorldImage helpText = new TextImage("Click to Restart", 10, Color.RED);

    //place background onto scene
    scene.placeImageXY(
        new RectangleImage(screenWidth, screenHeight, OutlineMode.SOLID, Color.DARK_GRAY), 
        screenWidth / 2, screenHeight / 2);

    if (!gameWon) {
      //place board onto scene
      WorldImage boardImage = this.drawGamePieces();
      scene.placeImageXY(boardImage, screenWidth / 2, screenHeight / 2);

      //place time and number of Moves onto screen
      scene.placeImageXY(timeBox, 25, screenHeight / 2);
      scene.placeImageXY(moveBox, screenWidth - 25, screenHeight / 2);
    }
    else {
      //place game won and help
      scene.placeImageXY(
          new AboveImage(gameWonText, helpText), screenWidth / 2, screenHeight / 2);

      //place time and moves
      scene.placeImageXY(new BesideImage(timeBox, moveBox),
          screenWidth / 2, screenHeight / 2 + 50);
    }
    return scene;
  }

  //computes the radius (longest-path in graph /2)
  public int computeRadius() {
    //run breadth-first search
    ArrayList<GamePiece> first = new BFS().start(this.nodes.get(0));
    //take last found node
    GamePiece lastFound = first.get(first.size() - 1);
    //run breadth-fist search again from last found node
    ArrayList<GamePiece> second = new BFS().start(lastFound);
    //find distance from the first and second lastfound nodes
    int diam = this.distFrom(
        lastFound, second.get(second.size() - 1), new ArrayList<GamePiece>());
    //divide by two to get radius
    return (int) Math.ceil(diam / 2.0);
  }

  //randomly rotates all pieces so the board is not trivial to solve
  public void randomizePieces() {
    for (GamePiece gp : this.nodes) {

      int count = 0;
      int numRotations = this.rand.nextInt(4);

      while (count <= numRotations) {
        gp.turnClockwise();
        count += 1;
      }
    }
  }
}

//a tile on the board
class GamePiece {
  // in logical coordinates, with the origin
  // at the top-left corner of the screen
  int row;
  int col;
  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;
  // whether the power station is on this piece
  boolean powerStation;
  //neighbors to the top, bottom, left, or right
  ArrayList<GamePiece> neighbors;

  //constructor
  GamePiece(boolean top, boolean bottom, boolean left, boolean right, 
      boolean pw, int col, int row) {
    this.top = top;
    this.bottom = bottom;
    this.left = left;
    this.right = right;
    this.row = row;
    this.col = col;
    this.powerStation = pw;
    this.neighbors = new ArrayList<GamePiece>();
  }

  //constructor for convenience
  GamePiece(int col, int row) {
    this(false, false, false, false, false, col, row);
  }


  //rotates the cell, moving all connections clockwise
  //returns 1 after turn
  public int turnClockwise() {
    GamePiece old = new GamePiece(this.top, this.bottom, this.left, 
        this.right, this.powerStation, 0, 0);
    this.top = old.left;
    this.right = old.top;
    this.bottom = old.right;
    this.left = old.bottom;
    return 1;
  }

  //returns an image of this gamepiece
  WorldImage draw(Color wireColor) {
    int cellSize = LightEmAll.CELL_SIZE;
    WorldImage cell = new RectangleImage(cellSize, cellSize, OutlineMode.OUTLINE, Color.BLACK);

    HashMap<String, Boolean> lineExists = new HashMap<String, Boolean>();
    lineExists.put("top", this.top);
    lineExists.put("bottom", this.bottom);
    lineExists.put("left", this.left);
    lineExists.put("right", this.right);

    //dictionary of offsets
    HashMap<String, ArrayList<Integer>> lineOffsets = LightEmAll.LINE_OFFSETS;

    for (String key : lineExists.keySet()) { //for each direction
      if (lineExists.get(key)) { // if true (connection exists)
        //overlay new rectangle with values from dictionary of offsets
        ArrayList<Integer> values = lineOffsets.get(key);
        cell = new OverlayOffsetImage(
            new RectangleImage(values.get(0), values.get(1), OutlineMode.SOLID, wireColor),
            values.get(2), 
            values.get(3), cell);
      }
      if (this.powerStation) { // if powerstation
        cell = new OverlayImage(LightEmAll.PW_STATION, cell);
      }
    }
    return cell;
  }

  //checks if two GamePieces are the same
  public boolean sameGP(GamePiece that) {
    return this.col == that.col
        && this.row == that.row;
  }

  //connects this GamePiece's wires to the given
  //have to be adjacent
  public void connectTo(GamePiece that) {
    if (that.row > this.row) {
      this.bottom = true;
      that.top = true;
    }
    else if (that.row < this.row) {
      this.top = true;
      this.bottom = false;
    }
    else if (that.col > this.col) {
      this.right = true;
      that.left = true;
    }
    else if (that.col < this.col) {
      this.left = true;
      that.right = true;
    }
  }
}

//breadth-first search 
class BFS {
  ArrayList<GamePiece> cameFrom;
  ArrayList<GamePiece> worklist;

  BFS() {
    this.cameFrom = new ArrayList<GamePiece>();
    this.worklist = new ArrayList<GamePiece>();
  }

  //returns the path taken 
  ArrayList<GamePiece> start(GamePiece prev) {
    this.cameFrom.add(prev);

    boolean allVisited = true;
    for (GamePiece neighbor : prev.neighbors) {
      if (!this.cameFrom.contains(neighbor)) {
        allVisited = false;
        worklist.add(neighbor);
      }
    }
    //base case (worklist empty)
    if (worklist.size() == 0) {
      return this.cameFrom;
    }
    //if all visited but worklist isn't empty
    if (allVisited) {
      this.cameFrom.remove(prev);
      return this.start(this.worklist.remove(0));
    }
    else {
      return this.start(this.worklist.remove(0));
    }
  }
}

//edge connecting nodes
class Edge {
  GamePiece fromNode;
  GamePiece toNode;
  int weight;

  Edge(GamePiece from, GamePiece to, int weight) {
    this.fromNode = from;
    this.toNode = to;
    this.weight = weight;
  }
}

//comparator object that compares edges by weight
class SortByWeight implements Comparator<Edge> {
  public int compare(Edge edge1, Edge edge2) {
    return edge1.weight - edge2.weight;
  }
}

class ExamplesLightEmAll {
  //LightEmAll part1 = new LightEmAll(8, 8, new Random(33), false);
  LightEmAll test;
  LightEmAll testFractal;
  LightEmAll testKruskal;
  LightEmAll test2Kruskal;
  LightEmAll testUnconnected;
  ArrayList<GamePiece> col0;
  ArrayList<GamePiece> col1;
  ArrayList<GamePiece> col2;
  ArrayList<GamePiece> col0f;
  ArrayList<GamePiece> col1f;
  ArrayList<GamePiece> col2f;
  ArrayList<GamePiece> col0k;
  ArrayList<GamePiece> col1k;
  ArrayList<GamePiece> col0k2;
  ArrayList<GamePiece> col1k2;
  ArrayList<GamePiece> col2k;
  ArrayList<GamePiece> col0kr;
  ArrayList<GamePiece> col1kr;
  ArrayList<GamePiece> col2kr;
  ArrayList<GamePiece> col0u;
  ArrayList<GamePiece> col1u;
  ArrayList<ArrayList<GamePiece>> test3x3;
  ArrayList<ArrayList<GamePiece>> test3x3Fractal;
  ArrayList<ArrayList<GamePiece>> test3x3Kruskal;
  ArrayList<ArrayList<GamePiece>> test3x3KruskalRand;
  ArrayList<ArrayList<GamePiece>> test2x2Kruskal;
  ArrayList<ArrayList<GamePiece>> test2x2Unconnected;
  WorldImage col0Im;
  WorldImage col1Im;
  WorldImage col2Im;
  WorldImage fullIm;
  WorldImage col0sc;
  WorldImage col1sc;
  WorldImage col2sc;
  WorldImage sceneIm;
  HashMap<GamePiece, GamePiece> reps;
  GamePiece g1;
  GamePiece g2;
  GamePiece g3;
  GamePiece g4;
  ArrayList<Edge> unsortedEdges;
  ArrayList<Edge> sortedEdges;
  Edge a;
  Edge b;
  Edge c;
  Edge d;

  //init tests
  void init() {
    testFractal = new LightEmAll(3, 3, true);
    test = new LightEmAll(3, 3, false);
    testKruskal = new LightEmAll(3, 3, new Random(33));

    //initialize everything for testkruskal again so everything is connected
    testKruskal.rand = new Random(33);
    testKruskal.initBoardKruskal(3, 3);
    testKruskal.placePowerStation(0, 0);
    testKruskal.connectNeighbors();


    col0 = new ArrayList<GamePiece>();
    col0.add(new GamePiece(false, true, false, false, false, 0, 0));
    col0.add(new GamePiece(true, true, false, true, false, 0, 1));
    col0.add(new GamePiece(true, false, false, false, false, 0, 2));

    col1 = new ArrayList<GamePiece>();
    col1.add(new GamePiece(false, true, false, false, false, 1, 0));
    col1.add(new GamePiece(true, true, true, true, false, 1, 1));
    col1.add(new GamePiece(true, false, false, false, false, 1, 2));

    col2 = new ArrayList<GamePiece>();
    col2.add(new GamePiece(false, true, false, false, false, 2, 0));
    col2.add(new GamePiece(true, true, true, false, false, 2, 1));
    col2.add(new GamePiece(true, false, false, false, false, 2, 2));

    col0f = new ArrayList<GamePiece>();
    col0f.add(new GamePiece(false, true, false, false, false, 0, 0));
    col0f.add(new GamePiece(true, true, false, true, false, 0, 1));
    col0f.add(new GamePiece(true, false, false, true, false, 0, 2));

    col1f = new ArrayList<GamePiece>();
    col1f.add(new GamePiece(false, true, false, false, false, 1, 0));
    col1f.add(new GamePiece(true, false, true, false, false, 1, 1));
    col1f.add(new GamePiece(false, false, true, true, false, 1, 2));

    col2f = new ArrayList<GamePiece>();
    col2f.add(new GamePiece(false, true, false, false, false, 2, 0));
    col2f.add(new GamePiece(true, true, false, false, false, 2, 1));
    col2f.add(new GamePiece(true, false, true, false, false, 2, 2));

    col0k = new ArrayList<GamePiece>();
    col0k.add(new GamePiece(false, true, false, true, false, 0, 0));
    col0k.add(new GamePiece(true, true, false, false, false, 0, 1));
    col0k.add(new GamePiece(true, false, false, true, false, 0, 2));

    col1k = new ArrayList<GamePiece>();
    col1k.add(new GamePiece(false, true, true, true, false, 1, 0));
    col1k.add(new GamePiece(true, false, false, false, false, 1, 1));
    col1k.add(new GamePiece(false, false, true, true, false, 1, 2));

    col2k = new ArrayList<GamePiece>();
    col2k.add(new GamePiece(false, true, true, false, false, 2, 0));
    col2k.add(new GamePiece(true, false, false, false, false, 2, 1));
    col2k.add(new GamePiece(false, false, true, false, false, 2, 2));

    col0kr = new ArrayList<GamePiece>();
    col0kr.add(new GamePiece(true, false, true, false, true, 0, 0));
    col0kr.add(new GamePiece(false, false, true, true, false, 0, 1));
    col0kr.add(new GamePiece(true, false, true, false, false, 0, 2));

    col1kr = new ArrayList<GamePiece>();
    col1kr.add(new GamePiece(false, true, true, true, false, 1, 0));
    col1kr.add(new GamePiece(false, false, false, true, false, 1, 1));
    col1kr.add(new GamePiece(false, false, true, true, false, 1, 2));

    col2kr = new ArrayList<GamePiece>();
    col2kr.add(new GamePiece(false, true, true, false, false, 2, 0));
    col2kr.add(new GamePiece(false, true, false, false, false, 2, 1));
    col2kr.add(new GamePiece(false, false, false, true, false, 2, 2));

    reps = new HashMap<GamePiece, GamePiece>();
    for (GamePiece node : testKruskal.nodes) {
      reps.put(node, node);
    }

    g1 = new GamePiece(false, true, false, true, false, 0, 0);
    g2 = new GamePiece(false, true, true, false, false, 0, 1);
    g3 = new GamePiece(true, false, false, true, false, 1, 0);
    g4 = new GamePiece(true, false, true, false, false, 1, 1);

    a = new Edge(g1, g2, 10);
    b = new Edge(g2, g4, 14);
    c = new Edge(g1, g3, 30);
    d = new Edge(g4, g3, 25);

    unsortedEdges = new ArrayList<Edge>(Arrays.asList(b, d, a, c));
    sortedEdges = new ArrayList<Edge>(Arrays.asList(a, b, d));


    col0k2 = new ArrayList<GamePiece>();
    col0k2.add(g1);
    col0k2.add(g2);

    col1k2 = new ArrayList<GamePiece>();
    col1k2.add(g3);
    col1k2.add(g4);

    test2x2Kruskal = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(col0k2, col1k2));

    test2Kruskal = new LightEmAll(test2x2Kruskal, 
        new ArrayList<GamePiece>(Arrays.asList(g1, g2, g3, g4)), 
        sortedEdges, 2, 2, 0, 0, 4, new Random(33));
    test2Kruskal.board = test2x2Kruskal;
    //testKruskal.initBoardKruskal(2, 2);

    testUnconnected = new LightEmAll(3, 3, false);
    col0u = new ArrayList<GamePiece>();
    col0u.add(new GamePiece(false, false, false, false, false, 0, 0));
    col0u.add(new GamePiece(false, false, false, false, false, 0, 1));

    col1u = new ArrayList<GamePiece>();
    col1u.add(new GamePiece(false, false, false, false, false, 1, 0));
    col1u.add(new GamePiece(false, false, false, false, false, 1, 1));

    test2x2Unconnected = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(col0u, col1u));

    testUnconnected.board = test2x2Unconnected;


    test3x3Kruskal = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(col0k, col1k, col2k));
    test3x3KruskalRand = new ArrayList<ArrayList<GamePiece>>(
        Arrays.asList(col0kr, col1kr, col2kr));

    test3x3 = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(col0, col1, col2));

    test3x3Fractal = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(col0f, col1f, col2f));

    test.initBoard(3, 3);

    col0Im = new AboveImage(
        new AboveImage(
            new AboveImage(
                new EmptyImage(),
                test.board.get(0).get(0).draw(Color.LIGHT_GRAY)),
            test.board.get(0).get(1).draw(Color.LIGHT_GRAY)), 
        test.board.get(0).get(2).draw(Color.LIGHT_GRAY));
    col1Im = new AboveImage(
        new AboveImage(
            new AboveImage(
                new EmptyImage(),
                test.board.get(1).get(0).draw(Color.LIGHT_GRAY)),
            test.board.get(1).get(1).draw(Color.LIGHT_GRAY)), 
        test.board.get(1).get(2).draw(Color.LIGHT_GRAY));
    col2Im = new AboveImage(
        new AboveImage(
            new AboveImage(
                new EmptyImage(),
                test.board.get(2).get(0).draw(Color.LIGHT_GRAY)),
            test.board.get(2).get(1).draw(Color.LIGHT_GRAY)), 
        test.board.get(2).get(2).draw(Color.LIGHT_GRAY));
    col0sc = new AboveImage(
        new AboveImage(
            new AboveImage(
                new EmptyImage(),
                test.board.get(0).get(0).draw(Color.LIGHT_GRAY)),
            test.board.get(0).get(1).draw(Color.LIGHT_GRAY)), 
        test.board.get(0).get(2).draw(Color.LIGHT_GRAY));
    col1sc = new AboveImage(
        new AboveImage(
            new AboveImage(
                new EmptyImage(),
                test.board.get(1).get(0).draw(Color.LIGHT_GRAY)),
            test.board.get(1).get(1).draw(Color.LIGHT_GRAY)), 
        test.board.get(1).get(2).draw(Color.LIGHT_GRAY));
    col2sc = new AboveImage(
        new AboveImage(
            new AboveImage(
                new EmptyImage(),
                test.board.get(2).get(0).draw(Color.LIGHT_GRAY)),
            test.board.get(2).get(1).draw(Color.LIGHT_GRAY)), 
        test.board.get(2).get(2).draw(Color.LIGHT_GRAY));
    fullIm = new BesideImage(
        new BesideImage(new BesideImage(new EmptyImage(), col0Im), col1Im), col2Im);
    sceneIm = new BesideImage(
        new BesideImage(new BesideImage(new EmptyImage(), col0sc), col1sc), col2sc);
  }


  void testinitBoard(Tester t) {
    init();
    t.checkExpect(test.board, test3x3);
    t.checkExpect(test.nodes.size(), 9);
    ArrayList<GamePiece> testNodes = new ArrayList<GamePiece>();
    testNodes.addAll(col0);
    testNodes.addAll(col1);
    testNodes.addAll(col2);
    t.checkExpect(test.nodes, testNodes);
  }

  void testconnectNeighbors(Tester t) {
    init();
    test.connectNeighbors();
    t.checkExpect(test.nodes.get(0).neighbors.size(), 1);
    t.checkExpect(test.nodes.get(0).neighbors, new ArrayList<GamePiece>(
        Arrays.asList(test.nodes.get(1))));
  }

  void testonMouseClick(Tester t) {
    init();
    test.initBoard(3, 3);
    t.checkExpect(test.nodes.get(0), new GamePiece(false, true, false, false, false, 0, 0));
    test.onMouseClicked(new Posn(51,0));
    t.checkExpect(test.nodes.get(0), new GamePiece(false, false, true, false, false, 0, 0));
    t.checkExpect(test.nodes.get(0).neighbors, new ArrayList<>());
  }

  void testonTick(Tester t) {
    init();
    test.time = 0;
    t.checkExpect(test.time, 0);
    test.onTick();
    t.checkExpect(test.time, 1);
    test.onTick();
    t.checkExpect(test.time, 2);
    test.gameWon = true;
    t.checkExpect(test.time, 2);
  }

  void testdrawAllGamePieces(Tester t) {
    init();
    test.connectNeighbors();
    t.checkExpect(test.drawGamePieces(), fullIm);
  }

  void testdraw(Tester t) {
    init();
    WorldImage outline = new RectangleImage(50, 50, OutlineMode.OUTLINE, Color.BLACK);
    t.checkExpect(col0.get(0).draw(Color.LIGHT_GRAY), new OverlayOffsetImage(
        new RectangleImage(3, 25, OutlineMode.SOLID, Color.LIGHT_GRAY),
        0, -12, outline));
    t.checkExpect(col0.get(1).draw(Color.LIGHT_GRAY), new OverlayOffsetImage(
        new RectangleImage(25, 3, OutlineMode.SOLID, Color.LIGHT_GRAY), -12, 0, 
        new OverlayOffsetImage(
            new RectangleImage(3, 25, OutlineMode.SOLID, Color.LIGHT_GRAY), 0, -12, 
            new OverlayOffsetImage(new RectangleImage(3, 25, OutlineMode.SOLID, Color.LIGHT_GRAY),
                0, 12, outline))));
  }

  void testTurnclockwise(Tester t) {
    init();
    GamePiece cell = this.col0.get(0);
    t.checkExpect(cell, new GamePiece(false, true, false, false, false,  0, 0));
    cell.turnClockwise();
    t.checkExpect(cell, new GamePiece(false, false, true, false, false, 0, 0));
    cell.turnClockwise();
    t.checkExpect(cell, new GamePiece(true, false, false, false, false, 0, 0));
    cell.turnClockwise();
    t.checkExpect(cell, new GamePiece(false, false, false, true, false, 0, 0));
    cell.turnClockwise();
    t.checkExpect(cell, new GamePiece(false, true, false, false, false, 0, 0));
  }

  void testinBounds(Tester t) {
    init();
    t.checkExpect(test.inBounds(new Posn(0,0)), false);
    t.checkExpect(test.inBounds(new Posn(50, 0)), true);
    t.checkExpect(test.inBounds(new Posn(51, 0)), true);
    t.checkExpect(test.inBounds(new Posn(200, 0)), true);
    t.checkExpect(test.inBounds(new Posn(201, 0)), false);
    t.checkExpect(test.inBounds(new Posn(51, 0)), true);
    t.checkExpect(test.inBounds(new Posn(51, 150)), true);
    t.checkExpect(test.inBounds(new Posn(51, 180)), false);
  }

  void testBFS(Tester t) {
    init();
    test.connectNeighbors();
    t.checkExpect(new BFS().start(test.nodes.get(0)), new ArrayList<GamePiece>(Arrays.asList(
        test.board.get(0).get(0), test.board.get(0).get(1), test.board.get(1).get(1), 
        test.board.get(2).get(1), test.board.get(2).get(0))));
  }

  void testMakeScene(Tester t) {
    init();
    test.moves = 1;
    WorldScene scene = new WorldScene(250, 150);
    WorldImage timeBox = new AboveImage(new TextImage("TIME", 20, Color.RED), 
        new TextImage("" + 0, 20, Color.RED));
    WorldImage moveBox = new AboveImage(new TextImage("MOVES", 14, Color.RED), 
        new TextImage("" + 1, 20, Color.RED));
    WorldImage gameWonText = new TextImage("You Won!", 20, Color.GREEN);
    WorldImage helpText = new TextImage("Click to Restart", 10, Color.RED);
    WorldImage bg = new RectangleImage(250, 150, OutlineMode.SOLID, Color.DARK_GRAY);

    //place background onto scene
    scene.placeImageXY(bg, 125, 75);
    scene.placeImageXY(sceneIm, 125, 75);
    scene.placeImageXY(timeBox, 25, 150 / 2);
    scene.placeImageXY(moveBox, 250 - 25, 150 / 2);
    t.checkExpect(test.makeScene(), scene);

    //testgameWon
    test.gameWon = true;

    scene = new WorldScene(250, 150);
    scene.placeImageXY(bg, 125, 75);
    scene.placeImageXY(new AboveImage(gameWonText, helpText), 125, 75);
    scene.placeImageXY(new BesideImage(timeBox, moveBox), 125, 125);
    t.checkExpect(test.makeScene(), scene);
  }

  void testdistFrom(Tester t) {
    ArrayList<GamePiece> mt = new ArrayList<>();
    init();
    test.connectNeighbors();
    t.checkExpect(test.distFrom(test.nodes.get(0), test.nodes.get(0), mt), 0);
    t.checkExpect(test.distFrom(test.board.get(0).get(0), test.board.get(2).get(2), mt), 4);
    test.nodes.get(0).turnClockwise();
    t.checkExpect(test.distFrom(test.nodes.get(0), test.nodes.get(8), mt), -1);
  }

  void testinitBoardFractal(Tester t) {
    init();
    testFractal.initBoardFractal(3, 3);
    t.checkExpect(testFractal.board, test3x3Fractal);
  }

  void testplacepowerstation(Tester t) {
    init();
    testFractal.placePowerStation(0, 0);
    t.checkExpect(testFractal.nodes.get(0).powerStation, true);
    t.checkExpect(testFractal.board.get(0).get(1).powerStation, false);
    testFractal.placePowerStation(0, 1);
    t.checkExpect(testFractal.board.get(0).get(1).powerStation, true);
  }

  void testfractalHelper(Tester t) {
    init();
    ArrayList<GamePiece> list = new ArrayList<GamePiece>();
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        list.add(new GamePiece(i, j));
      }
    }
    testFractal.fractcalHelper(list, 3, 3);
    ArrayList<GamePiece> testList = new ArrayList<GamePiece>();
    testList.addAll(col0f);
    testList.addAll(col1f);
    testList.addAll(col2f);
    t.checkExpect(list, testList);
  }

  void testsubdivide(Tester t) {
    init();
    ArrayList<GamePiece> list = new ArrayList<GamePiece>();
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        list.add(new GamePiece(i, j));
      }
    }
    testFractal.subdivide(list, 3, 3);
    ArrayList<GamePiece> testList = new ArrayList<GamePiece>();
    testList.addAll(col0f);
    testList.addAll(col1f);
    testList.addAll(col2f);
    t.checkExpect(list, testList);
  }

  void testComputeRadius(Tester t) {
    init();
    test.computeRadius();
    t.checkExpect(test.radius, 2);
    t.checkExpect(testFractal.computeRadius(), 4);
  }

  void testFind(Tester t) {
    init();
    t.checkExpect(testKruskal.find(reps, testKruskal.nodes.get(0)), testKruskal.nodes.get(0));
    t.checkExpect(testKruskal.find(reps, testKruskal.nodes.get(1)), testKruskal.nodes.get(1));
    testKruskal.union(reps, testKruskal.find(reps, testKruskal.nodes.get(0)), 
        testKruskal.find(reps, testKruskal.nodes.get(1)));
    testKruskal.union(reps, testKruskal.find(reps, testKruskal.nodes.get(1)), 
        testKruskal.find(reps, testKruskal.nodes.get(2)));
    t.checkExpect(testKruskal.find(reps, testKruskal.nodes.get(1)), testKruskal.nodes.get(0));
    t.checkExpect(testKruskal.find(reps, testKruskal.nodes.get(2)), testKruskal.nodes.get(0));

  }

  void testinitBoardKruskal(Tester t) {
    init();
    testKruskal.nodes.clear();
    testKruskal.board.clear();
    testKruskal.mst.clear();
    testKruskal.rand = new Random(33);
    testKruskal.initBoardKruskal(3, 3);
    for (GamePiece node : testKruskal.nodes) {
      node.neighbors = new ArrayList<GamePiece>();
    }
    t.checkExpect(testKruskal.board, test3x3Kruskal);
  }

  void testclearWires(Tester t) {
    init();
    t.checkExpect(test.nodes.get(0).bottom, true);
    t.checkExpect(test.nodes.get(8).top, true);
    t.checkExpect(test.nodes.get(4).bottom, true);
    t.checkExpect(test.nodes.get(4).left, true);
    t.checkExpect(test.nodes.get(4).right, true);
    t.checkExpect(test.nodes.get(4).top, true);
    test.clearWires();
    t.checkExpect(test.nodes.get(0).bottom, false);
    t.checkExpect(test.nodes.get(8).top, false);
    t.checkExpect(test.nodes.get(4).bottom, false);
    t.checkExpect(test.nodes.get(4).left, false);
    t.checkExpect(test.nodes.get(4).right, false);
    t.checkExpect(test.nodes.get(4).top, false);
  }

  void testrandomizePieces(Tester t) {
    init();
    testKruskal.randomizePieces();
    for (GamePiece gp : testKruskal.nodes) {
      gp.neighbors = new ArrayList<GamePiece>();
    }
    t.checkExpect(testKruskal.board, test3x3KruskalRand);
  }

  void testCreateEdges(Tester t) {
    LightEmAll edgeTest = new LightEmAll(3, 3, false);
    edgeTest.rand = new Random(33);
    Edge edge1 = new Edge(edgeTest.nodes.get(0), edgeTest.nodes.get(1), -1168181290);
    Edge edge2 = new Edge(edgeTest.nodes.get(1), edgeTest.nodes.get(2), -1247670057);
    Edge edge3 = new Edge(edgeTest.nodes.get(1), edgeTest.nodes.get(4), -706515689);
    Edge edge4 = new Edge(edgeTest.nodes.get(3), edgeTest.nodes.get(4), 182391814);
    Edge edge5 = new Edge(edgeTest.nodes.get(4), edgeTest.nodes.get(5), 1963797956);
    Edge edge6 = new Edge(edgeTest.nodes.get(4), edgeTest.nodes.get(7), 1314901947);
    Edge edge7 = new Edge(edgeTest.nodes.get(6), edgeTest.nodes.get(7), 1570576286);
    Edge edge8 = new Edge(edgeTest.nodes.get(7), edgeTest.nodes.get(8), -6840023);
    t.checkExpect(edgeTest.createEdges(), new ArrayList<Edge>(Arrays.asList(edge1, edge2, edge3,
        edge4, edge5, edge6, edge7, edge8)));
  }

  void testgenColors(Tester t) {
    init();
    test.genColors();
    HashMap<Integer, Color> colorMap = new HashMap<Integer, Color>();
    colorMap.put(0, new Color(255, 255, 0));
    colorMap.put(1, new Color(176, 159, 15));
    colorMap.put(2, new Color(97, 63, 30));
    t.checkExpect(test.lineColors, colorMap);
  }

  void testUnion(Tester t) {
    init();
    HashMap<GamePiece, GamePiece> reps = new HashMap<GamePiece, GamePiece>();
    reps.put(g1, g1);
    reps.put(g2,  g3);
    t.checkExpect(reps.get(g1), g1);
    t.checkExpect(reps.get(g2), g3);
    new LightEmAll(2, 2, false).union(reps, g2, g1);
    t.checkExpect(reps.get(g1), g2);
    t.checkExpect(reps.get(g2), g3);
    new LightEmAll(2, 2, false).union(reps, g4, g2);
    t.checkExpect(reps.get(g2), g4);
  }

  void testKruskalSort(Tester t) {
    init();
    t.checkExpect(test2Kruskal.kruskalsort(unsortedEdges), sortedEdges);
  }

  void testconnectTo(Tester t) {
    init();
    t.checkExpect(test2x2Unconnected.get(0).get(0).bottom, false);
    t.checkExpect(test2x2Unconnected.get(0).get(1).top, false);
    test2x2Unconnected.get(0).get(0).connectTo(test2x2Unconnected.get(0).get(1));
    t.checkExpect(test2x2Unconnected.get(0).get(0).bottom, true);
    t.checkExpect(test2x2Unconnected.get(0).get(1).top, true);

    t.checkExpect(test2x2Unconnected.get(0).get(0).right, false);
    t.checkExpect(test2x2Unconnected.get(1).get(0).left, false);
    test2x2Unconnected.get(0).get(0).connectTo(test2x2Unconnected.get(1).get(0));
    t.checkExpect(test2x2Unconnected.get(0).get(0).right, true);
    t.checkExpect(test2x2Unconnected.get(1).get(0).left, true);

    t.checkExpect(test2x2Unconnected.get(0).get(1).right, false);
    t.checkExpect(test2x2Unconnected.get(1).get(1).left, false);
    test2x2Unconnected.get(0).get(1).connectTo(test2x2Unconnected.get(1).get(1));
    t.checkExpect(test2x2Unconnected.get(0).get(1).right, true);
    t.checkExpect(test2x2Unconnected.get(1).get(1).left, true);

    t.checkExpect(test2x2Unconnected.get(1).get(0).bottom, false);
    t.checkExpect(test2x2Unconnected.get(1).get(1).top, false);
    test2x2Unconnected.get(1).get(0).connectTo(test2x2Unconnected.get(1).get(1));
    t.checkExpect(test2x2Unconnected.get(1).get(0).bottom, true);
    t.checkExpect(test2x2Unconnected.get(1).get(1).top, true);
  }

  void testdrawMst(Tester t) {
    init();
    test2Kruskal.mst = sortedEdges;
    test2Kruskal.drawMst();
    t.checkExpect(g1.bottom, true);
    t.checkExpect(g2.top, true);

    t.checkExpect(g2.right, true);
    t.checkExpect(g4.left, true);


    t.checkExpect(g4.top, true);
  }

  void testsameGP(Tester t) {
    init();
    t.checkExpect(g1.sameGP(g1), true);
    t.checkExpect(g1.sameGP(g2), false);
    t.checkExpect(g2.sameGP(g1), false);
  }

  void testGame(Tester t) {
    LightEmAll g = new LightEmAll(2, 2, true);
    LightEmAll g2 = new LightEmAll(4, 6, true);
    LightEmAll g3 = new LightEmAll(3, 3, true);
    LightEmAll g4 = new LightEmAll(3, 3, new Random(33));
    //        g.bigBang(200, 100, 1);
    //        g2.bigBang(300, 300, 1);
    g4.bigBang(500, 400, 1);
  }
}