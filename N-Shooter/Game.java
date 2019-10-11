import java.awt.Color;
import java.util.Random;
import javalib.funworld.*;
import javalib.worldimages.*;
import tester.*;

//represents the world for an N-Bullet Game
class NBulletGame extends World {
  int bulletsLeft; //represents the number of bullets left
  int shipsDestroyed; //represents the number of ships destroyed
  IList<IGameObject> shipsOnScreen; //represents the ships on screen
  IList<IGameObject> bulletsFired; //represents the bullets on screen
  int tick; //represents the ticks passed
  Random rand;

  static final int SCREEN_WIDTH = 500; //screen-width
  static final int SCREEN_HEIGHT = 300; //screen height
  static final int BULLET_SPEED = 8; // bullet speed
  static final int SHIP_SIZE = SCREEN_HEIGHT / 30; //radius of ship
  static final WorldScene EMPTY_SCENE = new WorldScene(SCREEN_WIDTH, SCREEN_HEIGHT);

  //static final Random RAND = new Random(33); IS this needed??

  //default constructor
  NBulletGame(int bulletsLeft, int shipsDestroyed, IList<IGameObject> shipsOnScreen, 
      IList<IGameObject> bulletsFired, int tick, Random rand) {
    this.bulletsLeft = bulletsLeft;
    this.shipsDestroyed = shipsDestroyed;
    this.shipsOnScreen = shipsOnScreen;
    this.bulletsFired = bulletsFired;
    this.tick = tick;
    this.rand = rand;
  }

  //constructor for new game
  NBulletGame(int nBullets) {
    this(nBullets, 0, new MtList<IGameObject>(), new MtList<IGameObject>(), 0, new Random());
  }

  //constructor to add tick
  NBulletGame(NBulletGame n) {
    this(n.bulletsLeft, n.shipsDestroyed, n.shipsOnScreen, n.bulletsFired, n.tick + 1, n.rand);
  }

  //to-draw method:
  //draw ships on empty-scene, then draw bullets, then draw score 
  public WorldScene makeScene() {
    WorldImage infoText = new TextImage(
        ("Bullets Left: " + this.bulletsLeft + ", Ships Destroyed: " +
            this.shipsDestroyed), 20, Color.BLACK);
    return this.bulletsFired.foldr(new PlaceAll(),
        this.shipsOnScreen.foldr(new PlaceAll(), EMPTY_SCENE)).
        placeImageXY(infoText, NBulletGame.SCREEN_WIDTH / 2, NBulletGame.SCREEN_HEIGHT - 10);
  }

  //shoots bullets up from center
  public NBulletGame onKeyEvent(String key) {
    if (this.bulletsLeft == 0) {
      return this;
    }
    else {
      return new NBulletGame(this.bulletsLeft - 1, this.shipsDestroyed, this.shipsOnScreen, 
          new ConsList<IGameObject>(new Bullet(), this.bulletsFired), this.tick, this.rand);
    }
  }

  //on-tick method
  //checks for end game, removeOffScreen and update bullets left, then checks for collisions,
  //then generates ships, then updates position
  public World onTick() {
    if (this.bulletsLeft == 0 && this.bulletsFired instanceof MtList<?>) {
      return this.endOfWorld("Game Over");
    }
    else {
      return 
          new NBulletGame(this. // add tick
              removeOffScreen(). //remove off screen objects
              collisionHandler(). //handle collisions
              generateShips(this.rand). //generate new ships
              moveObjects()); //move objects
    }
  }

  //removes items that are offscreen
  public NBulletGame removeOffScreen() {
    return new NBulletGame(this.bulletsLeft, this.shipsDestroyed,
        this.shipsOnScreen.filter(new RemoveOffScreen()),
        this.bulletsFired.filter(new RemoveOffScreen()), this.tick, this.rand);
  }

  //checks if there are any collisions between bullets and ships
  public NBulletGame collisionHandler() {
    return this.bulletsFired.foldr(new CollisionHandler(), this);
  }

  //generates between 1-3 ships randomly each second
  public NBulletGame generateShips(Random rand) {
    if ((this.tick % 28) == 0) {
      return new NBulletGame(
          this.bulletsLeft, this.shipsDestroyed, 
          new Ship().replicateObjects(rand, this.shipsOnScreen),
          this.bulletsFired, this.tick, this.rand);
    }
    else {
      return this;
    }
  }

  //moves ships and bullets 
  public NBulletGame moveObjects() {
    return new NBulletGame(
        this.bulletsLeft, this.shipsDestroyed, 
        this.shipsOnScreen.map(new UpdatePosition()),
        this.bulletsFired.map(new UpdatePosition()), this.tick, this.rand);
  }

}

//represents a position
class MyPosn extends Posn {

  //constructor
  MyPosn(int x, int y) {
    super(x, y); //inherit this.x, this.y from Posn class
  }

  //places the IGameObject onto given at this position
  public WorldScene placeOnImage(IGameObject object, WorldScene given) {
    return given.placeImageXY(object.draw(), this.x, this.y);
  }

  //moves the position by the given position
  public MyPosn movePosn(MyPosn vel) {
    return new MyPosn(this.x + vel.x, this.y + vel.y);
  }

  //generates random position for ships with constraints based on direction
  public MyPosn generateRandom(Random rand, boolean velIsLeft) {
    int level; //represents level above or below middle of screen 

    //determine the level below or above halfway the ship will spawn at
    boolean decideSign = rand.nextBoolean();
    if (decideSign) {
      level = rand.nextInt(6);
    }
    else {
      level = -1 * rand.nextInt(4);
    }

    //determine left or right side
    if (velIsLeft) {
      return new MyPosn(510, (25 * level) + (NBulletGame.SCREEN_HEIGHT / 3));
    }
    else {
      return new MyPosn(-10, (25 * level) + (NBulletGame.SCREEN_HEIGHT / 3));
    }
  }

  //checks if this position is outside given bounds
  public boolean isOffscreenHelp(int leftBound, int rightBound) {
    return this.x < leftBound
        || this.x > rightBound
        || this.y > NBulletGame.SCREEN_HEIGHT
        || this.y < (0 - NBulletGame.SHIP_SIZE);
  }

  //calculates the distance between this position and the given
  public double dist(MyPosn posn) {
    return Math.hypot(this.x - posn.x, this.y - posn.y);
  }  
}

//represents all visible objects on screen
interface IGameObject {

  //accepts 1 argument Function Objects for IGameObjects
  <R> R accept(IGameObjectVisitor<R> visitor);

  //accepts 2 arg Function objects for all IGameObjects
  <R> R accept2(IGameObjectVisitor2<R> visitor, R base);

  //draws object as WorldImage
  WorldImage draw();

  //checks if GameObject is offscreen
  boolean isOffScreen();

  //delegates to checkBullet/checkShip
  boolean checkCollision(IGameObject obj);

  //checks if there is a collision between this and given bullet
  boolean checkBullet(Bullet bullet);

  //checks if there is a collision between this and given ship
  boolean checkShip(Ship ship);

  //replicates this IGameObject and adds it onto given list of IGameObjects
  IList<IGameObject> replicateObjects(Random rand, IList<IGameObject> list);

  //helper for replicate IGameObject
  //for bullets: replicates based on level and changes velocities to emulate explosion
  //for ships: generates ships based on randomly generated number
  IList<IGameObject> replicateObjectsHelper(int level, IList<IGameObject> acc, int n, 
      Random rand);
}

//represents a bullet
class Bullet implements IGameObject {
  MyPosn posn; //position of the bullet
  MyPosn vel; //velocity of the bullet
  int size; //size of the bullet
  int level; // represents how many times its been replicated

  //constructor
  Bullet(MyPosn posn, MyPosn vel, int size, int level) {
    this.posn = posn;
    this.vel = vel;
    this.size = size;
    this.level = level;
  }

  //constructor for convenience
  Bullet() {
    this.posn = new MyPosn(NBulletGame.SCREEN_WIDTH / 2, NBulletGame.SCREEN_HEIGHT);
    this.vel = new MyPosn(0, -1 * NBulletGame.BULLET_SPEED);
    this.size = 2;
    this.level = 1;
  }

  //draws the bullet
  public WorldImage draw() {
    return new CircleImage(this.size, OutlineMode.SOLID, Color.PINK);
  }

  //acceptor for IFunc2 function objects
  public <R> R accept2(IGameObjectVisitor2<R> visitor, R base) {
    return visitor.visitBullet(this, base);
  }

  //checks if this bullet is off the screen
  public boolean isOffScreen() {
    int right = NBulletGame.SCREEN_WIDTH + this.size;
    int left = 0 - this.size;
    return this.posn.isOffscreenHelp(left, right);
  }

  //acceptor for IFunc function objects
  public <R> R accept(IGameObjectVisitor<R> visitor) {
    return visitor.visitBullet(this);
  }

  //delegates to checkBullet
  public boolean checkCollision(IGameObject obj) {
    return obj.checkBullet(this);
  }

  //checks if there is a collision with the given bullet
  //there cannot be, bullets cannot collide with each other
  public boolean checkBullet(Bullet bullet) {
    return false;
  }

  //checks is this bullet is colliding with given ship
  public boolean checkShip(Ship ship) {
    int minDist = this.size + NBulletGame.SHIP_SIZE;
    return (ship.posn.dist(this.posn) - minDist) < 0.001;
  }

  //replicate bullets based on level of bullet
  public IList<IGameObject> replicateObjects(Random rand, IList<IGameObject> bulletsFired) {
    int bulletLimit = 90; // maximum number of bullets that a single bullet can be replicated into
    if (this.level >= bulletLimit) {
      return this.replicateObjectsHelper(bulletLimit, bulletsFired, bulletLimit, rand);
    }
    return this.replicateObjectsHelper(this.level + 1, bulletsFired, this.level + 1, rand);
  }

  //overloaded method for true randomness
  public Object replicateObjects(IList<IGameObject> bulletsFired) {
    return this.replicateObjects(new Random(), bulletsFired);
  }

  //handles creating bullets that spawn off collisions
  public IList<IGameObject> replicateObjectsHelper(int level, IList<IGameObject> acc, int n, 
      Random rand) {
    double theta = level * (360 / n);
    int angleX = (int) (8 * Math.cos(Math.toRadians(theta)));
    int angleY = (int) (-8 * Math.sin(Math.toRadians(theta)));
    if (level == 0) {
      return acc;
    }
    else if (this.size == 10) {
      return new ConsList<IGameObject>(
          new Bullet(this.posn, new MyPosn(angleX, angleY), this.size, n),
          this.replicateObjectsHelper(level - 1, acc, n, rand));
    }
    else {
      return new ConsList<IGameObject>(
          new Bullet(this.posn, new MyPosn(angleX, angleY), this.size + 2, n),
          this.replicateObjectsHelper(level - 1, acc, n, rand));
    }
  }
}

//represents a ship to be destroyed
class Ship implements IGameObject {
  MyPosn posn; //represents the position of the ship
  boolean velIsLeft; //represents the direction the ship is moving

  //constructor
  Ship(MyPosn posn, boolean velIsLeft) {
    this.posn = posn;
    this.velIsLeft = velIsLeft;
  }

  //test-random ship constructor
  Ship(Random rand) {
    this.velIsLeft = rand.nextBoolean();
    this.posn = new MyPosn(0 , 0).generateRandom(rand, this.velIsLeft);
  }

  //truly random ship generator
  Ship() {
    this(new Random());
  }

  /*
   * TEMPLATE:
   * fields:
   * this.velIsLeft ... boolean
   * this.posn ... MyPosn
   * methods:
   * this.draw() ... WorldImage
   * this.place(WorldScene) ... WorldScene
   * this.acceptPlace(IGameObjectVisitor<WorldScene>, WorldScene) ... WorldScene
   * methods of fields:
   * this.posn.placeOnImage(IGameObject, WorldScene) ... WorldScene
   * this.posn.movePosn(MyPosn) ... MyPosn
   */

  //draws the ship as a WorldImage
  public WorldImage draw() {
    return new CircleImage(NBulletGame.SHIP_SIZE, OutlineMode.SOLID, Color.CYAN);
  }

  //accepts an IGameObject over a IFunc2 function object
  public <R> R accept2(IGameObjectVisitor2<R> visitor, R base) {
    return visitor.visitShip(this, base);
  }

  //checks if the ship is off the screen
  public boolean isOffScreen() {
    int leftBound = 0 - NBulletGame.SHIP_SIZE;
    int rightBound = NBulletGame.SCREEN_WIDTH + NBulletGame.SHIP_SIZE;
    return this.posn.isOffscreenHelp(leftBound, rightBound);
  }

  //acceptor for IFunc
  public <R> R accept(IGameObjectVisitor<R> visitor) {
    return visitor.visitShip(this);
  }

  //delegates to checkCollisionHelper
  public boolean checkCollision(IGameObject obj) {
    return obj.checkShip(this);
  }

  public boolean checkBullet(Bullet bullet) {
    int minDist = bullet.size + NBulletGame.SHIP_SIZE;
    return (this.posn.dist(bullet.posn) - minDist) < 0.001;
  }

  //returns false because ships cannot collide with other ships
  public boolean checkShip(Ship ship) {
    return false;
  }

  //generates a random number of new ships [1,3] and adds them onto list of ships
  public IList<IGameObject> replicateObjects(Random rand, IList<IGameObject> ships) {
    int numShips = 4; //upper bound for number of ships to generate at a time ([1, numShips])
    int level = rand.nextInt(numShips + 1);
    return this.replicateObjectsHelper(level, ships, 0, rand);
  }

  //overloaded method for truly random ships
  public IList<IGameObject> replicateObjects(IList<IGameObject> ships) {
    return this.replicateObjects(new Random(), ships);
  }

  //helper for replicateObjects, iterates over level
  public IList<IGameObject> replicateObjectsHelper(int level, IList<IGameObject> acc, int n, 
      Random rand) {
    if (level == 0) {
      return acc;
    }
    else {
      return new ConsList<IGameObject>(
          new Ship(rand), this.replicateObjectsHelper(level - 1, acc, 0, rand));
    }
  }
}

//an IGameObjectVisitor is an IFunc over IGameObjects
interface IGameObjectVisitor<R> extends IFunc<IGameObject, R> {
  
  R visitBullet(Bullet bullet);
  
  R visitShip(Ship ship);
  
  R apply(IGameObject input);
}

//an IGameObjectVisitor2 is an IFunc2 over IGameObjects 
interface IGameObjectVisitor2<R> extends IFunc2<IGameObject, R, R> {
  R visitBullet(Bullet bullet, R add);
  
  R visitShip(Ship ship, R add);
  
  R apply(IGameObject input, R base);
}

//function object that places all IGameObjects onto a scene
class PlaceAll implements IGameObjectVisitor2<WorldScene> {

  //places all bullets onto scene
  public WorldScene visitBullet(Bullet bullet, WorldScene scene) {
    return bullet.posn.placeOnImage(bullet, scene);
  }

  //places all ships onto the scene
  public WorldScene visitShip(Ship ship, WorldScene scene) {
    return ship.posn.placeOnImage(ship, scene);
  }

  //delegates IGameObject
  public WorldScene apply(IGameObject arg1, WorldScene arg2) {
    return arg1.accept2(this, arg2);
  }
}

//function object that updates positions of IGameObjects
class UpdatePosition implements IGameObjectVisitor<IGameObject> {

  //moves bullet
  public IGameObject visitBullet(Bullet bullet) {
    return new Bullet(bullet.posn.movePosn(bullet.vel), bullet.vel, bullet.size, bullet.level);
  }
  
  //moves ship
  public IGameObject visitShip(Ship ship) {
    if (ship.velIsLeft) {
      return new Ship(ship.posn.movePosn(new MyPosn(-4, 0)), ship.velIsLeft);
    }
    else {
      return new Ship(ship.posn.movePosn(new MyPosn(4, 0)), ship.velIsLeft);
    }
  }

  //delegates IGameObject
  public IGameObject apply(IGameObject input) {
    return input.accept(this);
  }
}

//function object that handles collisions with ships and bullets
//updates shipsDestroyed
//updates lists of ships and lists of bullets
class CollisionHandler implements IGameObjectVisitor2<NBulletGame> {

  //if iterating over a list of bullets
  public NBulletGame visitBullet(Bullet bullet, NBulletGame step) {
    NBulletGame updatedShips = step.shipsOnScreen.foldr(new CollisionHandlerHelper(bullet),
        new NBulletGame(step.bulletsLeft, step.shipsDestroyed, 
            new MtList<IGameObject>(), step.bulletsFired, step.tick, step.rand));
    if (updatedShips.shipsOnScreen.length() <= step.shipsOnScreen.length()) {
      return updatedShips;
    }
    else {
      return new NBulletGame(updatedShips.bulletsLeft, updatedShips.shipsDestroyed,
          updatedShips.shipsOnScreen, 
          new ConsList<IGameObject>(bullet, updatedShips.bulletsFired), step.tick, step.rand);
    }
  }

  //if iterating over a list of ships
  public NBulletGame visitShip(Ship ship, NBulletGame step) {
    NBulletGame updatedShips = step.bulletsFired.foldr(new CollisionHandlerHelper(ship),
        new NBulletGame(step.bulletsLeft, step.shipsDestroyed, 
            step.shipsOnScreen, new MtList<IGameObject>(), step.tick, step.rand));
    if (updatedShips.bulletsFired.length() < step.bulletsFired.length()) {
      return updatedShips;
    }
    else {
      return new NBulletGame(updatedShips.bulletsLeft, updatedShips.shipsDestroyed,
          new ConsList<IGameObject>(ship, updatedShips.shipsOnScreen),
          updatedShips.bulletsFired, updatedShips.tick, updatedShips.rand);
    }
  }

  //delegates to individual classes
  public NBulletGame apply(IGameObject input, NBulletGame step) {
    return input.accept2(this, step);
  }
}

//Function object that helps Collision handler (1 bullet, 1 ship at a time)
class CollisionHandlerHelper implements IFunc2<IGameObject, NBulletGame, NBulletGame> {
  IGameObject bullet;
  Random rand;

  //constructor
  CollisionHandlerHelper(Random rand, IGameObject bullet) {
    this.bullet = bullet;
    this.rand = rand;
  }
  
  //constructor for game
  CollisionHandlerHelper(IGameObject bullet) {
    this.bullet = bullet;
    this.rand = new Random();
  }

  //checks if ship collided with bullet, and if so, replicates bullets
  //if not, adds ship to list of ships on screen
  public NBulletGame apply(IGameObject ship, NBulletGame base) {
    if (ship.checkCollision(bullet)) {
      IList<IGameObject> noDuplicates = base.bulletsFired.filter(new RemoveBullet(bullet));
      return new NBulletGame(base.bulletsLeft, base.shipsDestroyed + 1, 
          base.shipsOnScreen, this.bullet.replicateObjects(rand, noDuplicates), 
          base.tick, base.rand);
    }
    else {
      return new NBulletGame(base.bulletsLeft, base.shipsDestroyed,
          new ConsList<IGameObject>(ship, base.shipsOnScreen),
          base.bulletsFired, base.tick, base.rand);
    }
  }
}

//predicate object that checks if IGameObject is offscreen
class RemoveOffScreen implements IPred<IGameObject> {

  //checks if IGameObject is offscreen
  public Boolean apply(IGameObject input) {
    return input.isOffScreen();
  }
}

//predicate object that checks if IGameObject is the same as given bullet
class RemoveBullet implements IPred<IGameObject> {
  IGameObject bullet;

  RemoveBullet(IGameObject bullet) {
    this.bullet = bullet;
  }
  
  //checks if this object is the given bullet
  public Boolean apply(IGameObject input) {
    return input.equals(bullet);
  }
}

class ExamplesNBulletGame {
  Random rand = new Random(33);
  PlaceAll placeAll = new PlaceAll();
  UpdatePosition updatePos = new UpdatePosition();
  CollisionHandler collHand = new CollisionHandler();
  RemoveOffScreen removeoffs = new RemoveOffScreen();


  WorldImage infoText = new TextImage(
      ("Bullets Left: " + 10 + ", Ships Destroyed: " +
          0), 20, Color.BLACK);

  WorldImage infoText2 = new TextImage(
      ("Bullets Left: " + 3 + ", Ships Destroyed: " +
          56), 20, Color.BLACK);

  MyPosn origin = new MyPosn(0, 0);
  MyPosn middle = new MyPosn(NBulletGame.SCREEN_WIDTH / 2, NBulletGame.SCREEN_HEIGHT / 2);
  MyPosn offScreen = new MyPosn(NBulletGame.SCREEN_WIDTH + 500, NBulletGame.SCREEN_HEIGHT + 500);
  MyPosn bottomLeft = new MyPosn(30, NBulletGame.SCREEN_HEIGHT - 30);
  MyPosn topLeft = new MyPosn(30, 30);
  MyPosn topRight = new MyPosn(NBulletGame.SCREEN_WIDTH - 30, 30);

  Ship ship1 = new Ship(this.middle, false);
  Ship ship2 = new Ship(this.offScreen, true);
  Ship shipColl = new Ship(this.topLeft, true);


  Bullet bullet1 = new Bullet(middle, middle, 2, 1);
  Bullet bullet1lv2p1 = new Bullet(middle,
      new MyPosn(-8, 0), 4, 2);
  Bullet bullet1lv2p2 = new Bullet(middle,
      new MyPosn(8, 0), 4, 2);
  IList<IGameObject> bullet1split = new ConsList<IGameObject>(bullet1lv2p2, 
      new ConsList<IGameObject>(bullet1lv2p1,
          new MtList<IGameObject>()));

  Bullet offSBullet = new Bullet(offScreen, middle, 2, 1);
  Bullet lev1Coll = new Bullet(this.topLeft, middle, 2, 1);


  Bullet bulletLv2 = new Bullet(middle, middle, 4, 2);
  Bullet bulletLv3p1 = new Bullet(middle, 
      new MyPosn((int)(8 * Math.cos(Math.toRadians(120))), 
          (int)(-8 * Math.sin(Math.toRadians(120)))), 6, 3);
  Bullet bulletLv3p2 = new Bullet(middle, 
      new MyPosn((int)(8 * Math.cos(Math.toRadians(240))), 
          (int)(-8 * Math.sin(Math.toRadians(240)))), 6, 3);
  Bullet bulletLv3p3 = new Bullet(middle, 
      new MyPosn((int)(8 * Math.cos(Math.toRadians(360))), 
          (int)(-8 * Math.sin(Math.toRadians(360)))), 6, 3);
  IList<IGameObject> bullet2split = new ConsList<IGameObject>(bulletLv3p3, 
      new ConsList<IGameObject>(bulletLv3p2,
          new ConsList<IGameObject>(bulletLv3p1,
              new MtList<IGameObject>())));

  IList<IGameObject> emptyLoShip = new MtList<IGameObject>();
  IList<IGameObject> emptyLoBullet = new MtList<IGameObject>();
  IList<IGameObject> loShip = 
      new ConsList<IGameObject>(ship1, 
          new ConsList<IGameObject>(ship2, 
              new ConsList<IGameObject>(shipColl, 
                  new MtList<IGameObject>())));
  IList<IGameObject> loBullets =
      new ConsList<IGameObject>(bullet1,
          new ConsList<IGameObject>(offSBullet,
              new ConsList<IGameObject>(bulletLv2,
                  new MtList<IGameObject>())));

  Ship randGen1 = new Ship(new MyPosn(510, 225), true);
  Ship randGen2 = new Ship(new MyPosn(-10, 75), false);
  IList<IGameObject> randGenShips = new ConsList<IGameObject>(randGen1,
      new ConsList<IGameObject>(randGen2, new MtList<IGameObject>()));

  NBulletGame new10Game = new NBulletGame(10);
  NBulletGame new10Gam2 = new NBulletGame(10, 0, new MtList<IGameObject>(),
      new MtList<IGameObject>(), 0, rand);
  NBulletGame inGame = new NBulletGame(3, 56, loShip, loBullets, 56, rand);

  RemoveBullet remBullet = new RemoveBullet(bullet1);
  RemoveBullet remBullet2 = new RemoveBullet(ship1);

  //test for makeScene()
  boolean testMakeScene(Tester t) {
    return t.checkExpect(this.new10Game.makeScene(), 
        NBulletGame.EMPTY_SCENE.placeImageXY(infoText, 250, 290))
        && t.checkExpect(this.inGame.makeScene(),
            inGame.bulletsFired.foldr(placeAll, 
                inGame.shipsOnScreen.foldr(placeAll, NBulletGame.EMPTY_SCENE)).
            placeImageXY(infoText2, 250, 290));
  }

  //test for onKeyEvent
  boolean testOnKey(Tester t) {
    return t.checkExpect(new10Game.onKeyEvent(" "), 
        new NBulletGame(new10Game.bulletsLeft - 1, new10Game.shipsDestroyed, 
            new10Game.shipsOnScreen, 
            new ConsList<IGameObject>(new Bullet(), new10Game.bulletsFired),
            new10Game.tick, new10Game.rand));
  }

  //test for onTick()
  boolean testOnTick(Tester t) {
    return t.checkExpect(new10Gam2.onTick(),
        new NBulletGame(10, 0, new ConsList<IGameObject>(new Ship(new MyPosn(506, 225), true),
            new ConsList<IGameObject>(new Ship(new MyPosn(-6, 75), false),
                new MtList<IGameObject>())), 
            new MtList<IGameObject>(), 1, new10Gam2.rand));
  }

  //test for removeOffScreen()
  boolean testremoveOffScreen(Tester t) {
    return t.checkExpect(inGame.removeOffScreen(),
        new NBulletGame(3, 56, inGame.shipsOnScreen.filter(removeoffs),
            inGame.bulletsFired.filter(removeoffs), 56, inGame.rand));
  }

  //test for collisionHandler()
  boolean testCollisionHandler(Tester t) {
    return t.checkExpect(new10Game.collisionHandler(), new10Game)
        && t.checkExpect(inGame.collisionHandler(), 
            inGame.bulletsFired.foldr(collHand, inGame));
  }

  //test for generateShips()
  boolean testGenerateShips(Tester t) {
    return t.checkExpect(new10Game.generateShips(new Random(33)), 
        new NBulletGame(10, 0, this.randGenShips, new10Game.bulletsFired, 0, new10Game.rand));
  }

  //test for moveObjects()
  boolean testmoveObjets(Tester t) {
    return t.checkExpect(inGame.moveObjects(), 
        new NBulletGame(3, 56, 
            new ConsList<IGameObject>(updatePos.visitShip(ship1), 
                new ConsList<IGameObject>(updatePos.visitShip(ship2), 
                    new ConsList<IGameObject>(updatePos.visitShip(shipColl), 
                        new MtList<IGameObject>()))),
            new ConsList<IGameObject>(updatePos.visitBullet(bullet1),
                new ConsList<IGameObject>(updatePos.visitBullet(offSBullet),
                    new ConsList<IGameObject>(updatePos.visitBullet(bulletLv2),
                        new MtList<IGameObject>()))), 
            56, inGame.rand));
  }

  //test for placeOnImage()
  boolean testplaceOnImage(Tester t) {
    return t.checkExpect(this.middle.placeOnImage(bullet1, NBulletGame.EMPTY_SCENE),
        NBulletGame.EMPTY_SCENE.placeImageXY(bullet1.draw(), middle.x, middle.y))
        && t.checkExpect(this.middle.placeOnImage(ship1, NBulletGame.EMPTY_SCENE),
            NBulletGame.EMPTY_SCENE.placeImageXY(ship1.draw(), middle.x, middle.y));
  }

  //rest for movePosn()
  boolean testmovePosn(Tester t) {
    return t.checkExpect(this.middle.movePosn(bottomLeft), new MyPosn(280 , 420))
        && t.checkExpect(this.topLeft.movePosn(middle), new MyPosn(280, 180));
  }

  //test for generateRandom()
  boolean testGenerateRandom(Tester t) {
    return t.checkExpect(middle.generateRandom(new Random(39), true), this.randGen1.posn);
  }

  //test for is OffScreenHelp()
  boolean testIsOffScreenHelper(Tester t) {
    return t.checkExpect(middle.isOffscreenHelp(0, 500), false)
        && t.checkExpect(offScreen.isOffscreenHelp(0, 500), true);
  }

  //test for dist
  boolean testDist(Tester t) {
    return t.checkInexact(origin.dist(middle), 291.54, 0.001)
        && t.checkExpect(origin.dist(new MyPosn(-1, -1)), Math.sqrt(2));
  }

  //test for accept
  boolean testAccept(Tester t) {
    return t.checkExpect(bullet1.accept(this.updatePos),
        new Bullet(new MyPosn(500,300), bullet1.vel, 2, 1))
        && t.checkExpect(ship1.accept(this.updatePos),
            new Ship(new MyPosn(254, 150), false));
  }

  //test for accept2
  boolean testAccept2(Tester t) {
    return t.checkExpect(bullet1.accept2(collHand, new10Game), new10Game);
  }



  //test for IGameObject.draw()
  boolean testDraw(Tester t) {
    return t.checkExpect(this.ship1.draw(), 
        new CircleImage(NBulletGame.SHIP_SIZE, OutlineMode.SOLID, Color.CYAN))
        && t.checkExpect(this.ship2.draw(), 
            new CircleImage(NBulletGame.SHIP_SIZE, OutlineMode.SOLID, Color.CYAN))
        && t.checkExpect(this.bullet1.draw(), 
            new CircleImage(this.bullet1.size, OutlineMode.SOLID, Color.PINK))
        && t.checkExpect(this.bulletLv2.draw(), 
            new CircleImage(this.bulletLv2.size, OutlineMode.SOLID, Color.PINK));
  }

  //test for IGameObject.isOffScreen()
  boolean testisOffScreen(Tester t) {
    return t.checkExpect(offSBullet.isOffScreen(), true)
        && t.checkExpect(bullet1.isOffScreen(), false)
        && t.checkExpect(ship1.isOffScreen(), false)
        && t.checkExpect(ship2.isOffScreen(), true);
  }

  //test for checkBullet
  boolean testcheckBullet(Tester t) {
    return t.checkExpect(this.bullet1.checkBullet(bullet1), false)
        && t.checkExpect(this.bullet1.checkBullet(bulletLv2), false)
        && t.checkExpect(this.ship1.checkBullet(bullet1), true)
        && t.checkExpect(this.ship1.checkBullet(offSBullet), false);
  }

  //test for checkShip
  boolean testCheckShip(Tester t) {
    return t.checkExpect(this.bullet1.checkShip(ship1), true)
        && t.checkExpect(this.bullet1.checkShip(ship2), false)
        && t.checkExpect(this.ship1.checkShip(ship1), false)
        && t.checkExpect(this.ship1.checkShip(ship2), false);
  }

  //test for checkCollision
  boolean testcheckCollision(Tester t) {
    return t.checkExpect(this.bullet1.checkCollision(bullet1), false)
        && t.checkExpect(this.bullet1.checkCollision(bulletLv2), false)
        && t.checkExpect(this.bullet1.checkCollision(ship1), true)
        && t.checkExpect(this.bullet1.checkCollision(ship2), false)
        && t.checkExpect(this.ship1.checkCollision(bullet1), true)
        && t.checkExpect(this.ship1.checkCollision(offSBullet), false)
        && t.checkExpect(this.ship1.checkCollision(ship1), false)
        && t.checkExpect(this.ship1.checkCollision(ship2), false);
  }

  //test for replicateObjects
  boolean testreplicateObjects(Tester t) {
    return t.checkExpect(this.bullet1.replicateObjects(emptyLoBullet), this.bullet1split)
        && t.checkExpect(this.bulletLv2.replicateObjects(emptyLoBullet), this.bullet2split)
        && t.checkExpect(this.ship1.replicateObjects(new Random(33), emptyLoShip), 
            this.randGenShips);
  }

  //test for replicateObjectsHelper
  boolean testreplicateObjectsHelper(Tester t) {
    return t.checkExpect(this.bullet1.replicateObjectsHelper(
        bullet1.level + 1, this.emptyLoBullet, bullet1.level + 1, rand), this.bullet1split)
        && t.checkExpect(this.bulletLv2.replicateObjectsHelper(
            bulletLv2.level + 1, this.emptyLoBullet, bulletLv2.level + 1, rand), 
            this.bullet2split)
        && t.checkExpect(this.ship1.replicateObjectsHelper(2, this.emptyLoShip, 0, new Random(33)),
            this.randGenShips);
  }

  //tests for PlaceAll
  boolean testPlaceAll(Tester t) {
    return t.checkExpect(placeAll.visitBullet(bullet1, NBulletGame.EMPTY_SCENE), 
        NBulletGame.EMPTY_SCENE.placeImageXY(bullet1.draw(), bullet1.posn.x, bullet1.posn.y))
        && t.checkExpect(placeAll.visitShip(ship1, NBulletGame.EMPTY_SCENE), 
            NBulletGame.EMPTY_SCENE.placeImageXY(ship1.draw(), ship1.posn.x, ship1.posn.y));
  }

  //tests for UpdatePosition
  boolean testUpdatePosition(Tester t) {
    return t.checkExpect(updatePos.visitBullet(bullet1), 
        new Bullet(bullet1.posn.movePosn(bullet1.vel), bullet1.vel, bullet1.size, bullet1.level))
        && t.checkExpect(updatePos.visitShip(ship1), 
            new Ship(ship1.posn.movePosn(new MyPosn(4, 0)), ship1.velIsLeft));
  }

  //tests for RemoveOffScreen
  boolean testremoveoffscreen(Tester t) {
    return t.checkExpect(removeoffs.apply(offSBullet), true)
        && t.checkExpect(removeoffs.apply(bullet1), false)
        && t.checkExpect(removeoffs.apply(ship2), true)
        && t.checkExpect(removeoffs.apply(ship1), false);
  }

  //tests for RemoveBullet
  boolean testremovebullet(Tester t) {
    return t.checkExpect(remBullet.apply(bullet1), true)
        && t.checkExpect(remBullet.apply(ship1), false)
        && t.checkExpect(remBullet.apply(offSBullet), false)
        && t.checkExpect(remBullet2.apply(ship1), true)
        && t.checkExpect(remBullet2.apply(bullet1), false);
  }

  //big-bang tester
  boolean testBigBang(Tester t) {
    NBulletGame w = new NBulletGame(10);
    int worldWidth = NBulletGame.SCREEN_WIDTH;
    int worldHeight = NBulletGame.SCREEN_HEIGHT;
    double tickRate = 1.0 / 28.0;
    return w.bigBang(worldWidth, worldHeight, tickRate);
  }
}