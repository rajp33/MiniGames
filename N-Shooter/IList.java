import tester.*;

//represents a list
public interface IList<T> {
  //maps a function onto all T in a list
  <U> IList<U> map(IFunc<T, U> func);
  
  //checks if at least one item in the list returns true for predicate
  Boolean ormap(IPred<T> pred);

  //folds all items in list to Return type
  <U> U foldr(IFunc2<T, U, U> func, U base);

  //filters list based on if predicate returns true
  IList<T> filter(IPred<T> pred);

  //returns the length of the list
  int length();
  
  //accepts IFunc<T, R> (one arg) Visitors
  <U> U accept(IListVisitor<T, U> input);
}

//represents an empty list of T
class MtList<T> implements IList<T> {

  //maps a function onto the empty list
  public <U> IList<U> map(IFunc<T, U> func) {
    return new MtList<U>();
  }

  //returns base
  public <U> U foldr(IFunc2<T, U, U> func, U base) {
    return base;
  }

  //returns 0 because an empty list has no length
  public int length() {
    return 0;
  }

  //returns this
  public IList<T> filter(IPred<T> pred) {
    return this;
  }

  //accepts MtList<T> Objects
  public <U> U accept(IListVisitor<T, U> input) {
    return input.visitMt(this);
  }

  //empty-list returns false
  public Boolean ormap(IPred<T> pred) {
    return false;
  }
}

//represents a non-empty list of T
class ConsList<T> implements IList<T> {
  T first;
  IList<T> rest;

  //constructor
  ConsList(T first, IList<T> rest) {
    this.first = first;
    this.rest = rest;
  }

  /*
   * TEMPLATE:
   * fields:
   * this.first ... T
   * this.rest ... IList<T>
   * methods:
   * this.map ... IList<U>
   * this.foldr ... <U>
   * methods of fields:
   * this.rest.map ... IList<U>
   * this.rest.foldr ... <U>
   */

  //maps a function onto the non-empty list
  public <U> IList<U> map(IFunc<T, U> func) {
    return new ConsList<U>(func.apply(this.first), this.rest.map(func));
  }

  //folds the items in the list into return type U
  public <U> U foldr(IFunc2<T, U, U> func, U base) {
    return func.apply(this.first, this.rest.foldr(func, base));
  }

  //adds 1 for each item in list
  public int length() {
    return 1 + this.rest.length();
  }

  //filters based on predicate (if true then removes it)
  public IList<T> filter(IPred<T> pred) {
    return (pred.apply(first) ? 
        this.rest.filter(pred) : new ConsList<T>(this.first, this.rest.filter(pred)));
  }

  //accepts a ConsList<T> object
  public <U> U accept(IListVisitor<T, U> input) {
    return input.visitCons(this);
  }

  //checks each item in list with predicate, if one returns true, returns true
  public Boolean ormap(IPred<T> pred) {
    return pred.apply(first)
        || this.rest.ormap(pred);
  }
}

//interface for predicate object that takes in type A and returns a boolean
interface IPred<A> extends IFunc<A, Boolean> {
  Boolean apply(A input);
}

//interface for function object that takes in Argument A and returns R
interface IFunc<A, R> {
  R apply(A input);
}

//interface for function object that takes in two arguments A and returns R
interface IFunc2<A1, A2, R> {
  public R apply(A1 arg1, A2 arg2);
}

//visitor pattern for ILists over IFunc<T, R>
interface IListVisitor<T, R> extends IFunc<T, R> {

  //visit a non-empty list
  R visitCons(ConsList<T> input);

  //visit an empty list;
  R visitMt(MtList<T> input);
}


//function objects to use for tests

//squares each item in list
class Sqr implements IFunc<Integer, Integer> {
  public Integer apply(Integer input) {
    return input * input;
  }
}

//checks if numbers are even
class Even implements IPred<Integer> {
  public Boolean apply(Integer input) {
    return (input % 2) == 0;
  }
}

//sums all numbers in list
class Sum implements IFunc2<Integer, Integer, Integer> {
  public Integer apply(Integer arg1, Integer arg2) {
    return arg1 + arg2;
  }

}

class ExamplesIList {
  Sum sum = new Sum();
  Even even = new Even();
  Sqr square = new Sqr();
  IList<Integer> empty = new MtList<Integer>();
  IList<Integer> zeroTen = new ConsList<Integer>(0,
      new ConsList<Integer>(1,
          new ConsList<Integer>(2,
              new ConsList<Integer>(3,
                  new ConsList<Integer>(4,
                      new ConsList<Integer>(5,
                          new ConsList<Integer>(6,
                              new ConsList<Integer>(7,
                                  new ConsList<Integer>(8,
                                      new ConsList<Integer>(9,
                                          new ConsList<Integer>(10,
                                              new MtList<Integer>())))))))))));
  IList<Integer> zeroTenSqrd = new ConsList<Integer>(0,
      new ConsList<Integer>(1,
          new ConsList<Integer>(4,
              new ConsList<Integer>(9,
                  new ConsList<Integer>(16,
                      new ConsList<Integer>(25,
                          new ConsList<Integer>(36,
                              new ConsList<Integer>(49,
                                  new ConsList<Integer>(64,
                                      new ConsList<Integer>(81,
                                          new ConsList<Integer>(100,
                                              new MtList<Integer>())))))))))));
  IList<Integer> zeroTenEven = new ConsList<Integer>(2,
      new ConsList<Integer>(4,
          new ConsList<Integer>(6,
              new ConsList<Integer>(8,
                  new ConsList<Integer>(10,
                      new MtList<Integer>())))));
  IList<Integer> zeroTenOdd = new ConsList<Integer>(1,
      new ConsList<Integer>(3,
          new ConsList<Integer>(5,
              new ConsList<Integer>(7,
                  new ConsList<Integer>(9,
                      new MtList<Integer>())))));

  //test Sqr()
  boolean testSqr(Tester t) {
    return t.checkExpect(square.apply(0), 0)
        && t.checkExpect(square.apply(1), 1)
        && t.checkExpect(square.apply(2), 4)
        && t.checkExpect(square.apply(-1), 1)
        && t.checkExpect(square.apply(-4), 16);
  }

  //test Even()
  boolean testEven(Tester t) {
    return t.checkExpect(even.apply(0), true)
        && t.checkExpect(even.apply(1), false)
        && t.checkExpect(even.apply(2), true)
        && t.checkExpect(even.apply(3), false)
        && t.checkExpect(even.apply(-2), true)
        && t.checkExpect(even.apply(-3), false);
  }

  //test Sum()
  boolean testSum(Tester t) {
    return t.checkExpect(sum.apply(0, 1), 1)
        && t.checkExpect(sum.apply(1, 2), 3)
        && t.checkExpect(sum.apply(2, 3), 5)
        && t.checkExpect(sum.apply(-3, 4), 1)
        && t.checkExpect(sum.apply(4, 5), 9);
  }

  //test map
  boolean testMap(Tester t) {
    return t.checkExpect(this.empty.map(new Sqr()), this.empty)
        && t.checkExpect(this.zeroTen.map(new Sqr()), this.zeroTenSqrd);
  }

  //test filter
  boolean testFilter(Tester t) {
    return t.checkExpect(this.empty.filter(new Even()), this.empty)
        && t.checkExpect(this.zeroTen.filter(new Even()), this.zeroTenOdd);
  }

  //test foldr
  boolean testFoldr(Tester t) {
    return t.checkExpect(this.empty.foldr(new Sum(), 0), 0)
        && t.checkExpect(this.zeroTen.foldr(new Sum(), 0), 55);
  }

  //test length
  boolean testLength(Tester t) {
    return t.checkExpect(this.zeroTen.length(), 11)
        && t.checkExpect(this.empty.length(), 0);
  }
}
