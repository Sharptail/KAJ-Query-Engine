/**
 * Tuple container class
 **/

package qp.utils;

import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

/**
 * Tuple - a simple object which holds an ArrayList of data
 */
public class Tuple implements Serializable {

    public ArrayList<Object> _data;

    public Tuple(ArrayList<Object> d) {
        _data = d;
    }

    /**
     * Accessor for data
     */
    public ArrayList<Object> data() {
        return _data;
    }

    public Object dataAt(int index) {
        return _data.get(index);
    }

    /**
     * Checks whether the join condition is satisfied or not with one condition
     * * before performing actual join operation
     **/
    public boolean checkJoin(Tuple right, int leftindex, int rightindex) {
        Object leftData = dataAt(leftindex);
        Object rightData = right.dataAt(rightindex);
        if (leftData.equals(rightData))
            return true;
        else
            return false;
    }

    /**
     * Checks whether the join condition is satisfied or not with multiple conditions
     * * before performing actual join operation
     **/
    public boolean checkJoin(Tuple right, ArrayList<Integer> leftindex, ArrayList<Integer> rightindex) {
        if (leftindex.size() != rightindex.size())
            return false;
        for (int i = 0; i < leftindex.size(); ++i) {
            Object leftData = dataAt(leftindex.get(i));
            Object rightData = right.dataAt(rightindex.get(i));
            if (!leftData.equals(rightData)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Joining two tuples without duplicate column elimination
     **/
    public Tuple joinWith(Tuple right) {
        ArrayList<Object> newData = new ArrayList<>(this.data());
        newData.addAll(right.data());
        return new Tuple(newData);
    }

    /**
     * Compare whether two tuples are the same in the same table for every attribute
     */
    public boolean isEquals(Tuple tuple){
        for(int i = 0; i < this._data.size(); i++){
            if(compareTuples(this, tuple, i) != 0){
                return false;
            }
        }
        return true;
    }

    /**
     * Compare two tuples in the same table on given attribute
     **/
    public static int compareTuples(Tuple left, Tuple right, int index) {
        return compareTuples(left, right, index, index);
    }

    /**
     * Comparing tuples in different tables, used for join condition checking
     **/
    public static int compareTuples(Tuple left, Tuple right, int leftIndex, int rightIndex) {
        Object leftdata = left.dataAt(leftIndex);
        Object rightdata = right.dataAt(rightIndex);
        if (leftdata instanceof Integer) {
            return ((Integer) leftdata).compareTo((Integer) rightdata);
        } else if (leftdata instanceof String) {
            return ((String) leftdata).compareTo((String) rightdata);
        } else if (leftdata instanceof Float) {
            return ((Float) leftdata).compareTo((Float) rightdata);
        } else {
            System.out.println("Tuple: Unknown comparision of the tuples");
            System.exit(1);
            return 0;
        }
    }

    /**
     * Comparing tuples in different tables with multiple conditions, used for join condition checking
     **/
    public static int compareTuples(Tuple left, Tuple right, List<Integer> leftIndex, List<Integer> rightIndex) {
        if (leftIndex.size() != rightIndex.size()) {
            System.out.println("Tuple: Unknown comparision of the tuples");
            System.exit(1);
            return 0;
        }
        for (int i = 0; i < leftIndex.size(); ++i) {
            Object leftdata = left.dataAt(leftIndex.get(i));
            Object rightdata = right.dataAt(rightIndex.get(i));
            if (leftdata.equals(rightdata)) continue;
            if (leftdata instanceof Integer) {
                return ((Integer) leftdata).compareTo((Integer) rightdata);
            } else if (leftdata instanceof String) {
                return ((String) leftdata).compareTo((String) rightdata);
            } else if (leftdata instanceof Float) {
                return ((Float) leftdata).compareTo((Float) rightdata);
            } else {
                System.out.println("Tuple: Unknown comparision of the tuples");
                System.exit(1);
                return 0;
            }
        }
        return 0;
    }

    /**
     * Parse the array indexes to List to check for join condition
     */
    public static int compareTuples(Tuple left, Tuple right, int[] leftIndex, int[] rightIndex) {
        List<Integer> leftIdx = Arrays.stream(leftIndex).boxed().collect(Collectors.toList());
        List<Integer> rightIdx = Arrays.stream(rightIndex).boxed().collect(Collectors.toList());
        return compareTuples(left, right, leftIdx, rightIdx);
    }
}
