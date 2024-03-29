package qp.operators;

import java.util.ArrayList;

import qp.utils.Attribute;
import qp.utils.Batch;
import qp.utils.Condition;
import qp.utils.Tuple;

/**
 * Sort Merge Join Algorithm
 */
public class SortMergeJoin extends Join {
    int batchsize;                  // Number of tuples per out batch
    ExternalSort leftsort;          // Sort Operator on left
    ExternalSort rightsort;         // Sort Operator on right
    ArrayList<Integer> leftindex;   // Indices of the join attributes in left table
    ArrayList<Integer> rightindex;  // Indices of the join attributes in right table
    ArrayList<Tuple> temp;          // Temporary ArrayList of Tuples

    Batch outbatch;                 // Buffer page for output
    Batch leftbatch;                // Buffer page for left input stream
    Batch rightbatch;               // Buffer page for right input stream
    int lcurs;                      // Cursor for left side buffer
    int rcurs;                      // Cursor for right side buffer
    int tempcurs = -1;              // Temporary cursor
    boolean eos;                    // Indicate whether end of stream is reached or not

    public SortMergeJoin(Join jn) {
        super(jn.getLeft(), jn.getRight(), jn.getCondition(), jn.getOpType());
        schema = jn.getSchema();
        jointype = jn.getJoinType();
        numBuff = jn.getNumBuff();
        leftindex = new ArrayList<>();
        rightindex = new ArrayList<>();
        temp = new ArrayList<>();
    }

    @Override
    public boolean open() {
        int tupleSize = schema.getTupleSize();
        this.batchsize = Batch.getPageSize() / tupleSize;

        /** Throw error if page size is smaller than tuple size */
        if (batchsize < 1) {
            System.err.println("Error: Page size must be bigger than tuple size for joining.");
            return false;
        }

        /** find indices attributes of join conditions **/
        for (Condition con : this.conditionList) {
            Attribute leftattr = con.getLhs();
            Attribute rightattr = (Attribute) con.getRhs();
            this.leftindex.add(left.getSchema().indexOf(leftattr));
            this.rightindex.add(right.getSchema().indexOf(rightattr));
        }

        /** initialize the cursors of input buffers **/
        lcurs = 0;
        rcurs = 0;
        eos = false;
        leftsort = new ExternalSort(left, numBuff, leftindex, "left");
        rightsort = new ExternalSort(right, numBuff, rightindex, "right");

        if (!leftsort.open() || !rightsort.open()) {
            return false;
        }

        leftbatch = leftsort.next();
        rightbatch = rightsort.next();

        return true;
    }

    public Batch getBlock(int sizeofblock) {
        outbatch = new Batch(sizeofblock);
        while (leftbatch != null && rightbatch != null) {
            Tuple lefttuple = leftbatch.get(lcurs);
            Tuple righttuple = getRightTuple();

            if (tempcurs == -1) {
                while (Tuple.compareTuples(lefttuple, righttuple, leftindex, rightindex) < 0) {
                    lcurs++;
                    if (leftbatch != null && lcurs >= leftbatch.size()) {
                        leftbatch = leftsort.next();
                        lcurs = 0;
                    }
                    if (leftbatch == null) break;
                    lefttuple = leftbatch.get(lcurs);
                }

                while (Tuple.compareTuples(lefttuple, righttuple, leftindex, rightindex) > 0) {
                    rcurs++;
                    if (rightbatch != null && rcurs >= rightbatch.size() + temp.size()) {
                        temp.addAll(rightbatch.getTuples());
                        rightbatch = rightsort.next();
                    }
                    if (rightbatch == null) break;
                    righttuple = getRightTuple();
                }
                if (rcurs >= temp.size()) {
                    rcurs -= temp.size();
                }
                tempcurs = rcurs;
                temp.clear();
            }

            if (Tuple.compareTuples(lefttuple, righttuple, leftindex, rightindex) == 0) {
                outbatch.add(lefttuple.joinWith(righttuple));
                rcurs++;
                if (rightbatch != null && rcurs >= rightbatch.size() + temp.size()) {
                    temp.addAll(rightbatch.getTuples());
                    rightbatch = rightsort.next();
                }
                if (rightbatch == null) break;
                if (outbatch.isFull()) {
                    return outbatch;
                }
            } else {
                rcurs = tempcurs;
                lcurs++;
                if (leftbatch != null && lcurs >= leftbatch.size()) {
                    leftbatch = leftsort.next();
                    lcurs = 0;
                }
                if (leftbatch == null) break;
                tempcurs = -1;
            }
        }

        if (outbatch.isEmpty()) {
            close();
            return null;
        } else {
            return outbatch;
        }
    }

    /**
     * Performs the comparison of sorted tuples by advancing the left and right pointers
     * If there is a match, write to the output buffer
     */
    @Override
    public Batch next() {
        outbatch = new Batch(batchsize);
        while (leftbatch != null && rightbatch != null) {
            Tuple lefttuple = leftbatch.get(lcurs);
            Tuple righttuple = getRightTuple();

            if (tempcurs == -1) {
                while (Tuple.compareTuples(lefttuple, righttuple, leftindex, rightindex) < 0) {
                    lcurs++;
                    if (leftbatch != null && lcurs >= leftbatch.size()) {
                        leftbatch = leftsort.next();
                        lcurs = 0;
                    }
                    if (leftbatch == null) break;
                    lefttuple = leftbatch.get(lcurs);
                }

                while (Tuple.compareTuples(lefttuple, righttuple, leftindex, rightindex) > 0) {
                    rcurs++;
                    if (rightbatch != null && rcurs >= rightbatch.size() + temp.size()) {
                        temp.addAll(rightbatch.getTuples());
                        rightbatch = rightsort.next();
                    }
                    if (rightbatch == null) break;
                    righttuple = getRightTuple();
                }
                if (rcurs >= temp.size()) {
                    rcurs -= temp.size();
                }
                tempcurs = rcurs;
                temp.clear();
            }

            if (Tuple.compareTuples(lefttuple, righttuple, leftindex, rightindex) == 0) {
                outbatch.add(lefttuple.joinWith(righttuple));
                rcurs++;
                if (rightbatch != null && rcurs >= rightbatch.size() + temp.size()) {
                    temp.addAll(rightbatch.getTuples());
                    rightbatch = rightsort.next();
                }
                if (rightbatch == null) break;
                if (outbatch.isFull()) {
                    return outbatch;
                }
            } else {
                rcurs = tempcurs;
                lcurs++;
                if (leftbatch != null && lcurs >= leftbatch.size()) {
                    leftbatch = leftsort.next();
                    lcurs = 0;
                }
                if (leftbatch == null) break;
                tempcurs = -1;
            }
        }

        if (outbatch.isEmpty()) {
            close();
            return null;
        } else {
            return outbatch;
        }
    }

    /**
     * Retrieval of Right Tuple
     * @return rightTuple
     */
    private Tuple getRightTuple() {
        if (temp.size() == 0) {
            return rightbatch.get(rcurs);
        } else {
            if (rcurs < temp.size()) {
                return temp.get(rcurs);
            } else {
                return rightbatch.get(rcurs - temp.size());
            }
        }
    }

    /**
     * Close the operator
     */
    @Override
    public boolean close() {
        leftsort.close();
        rightsort.close();
        return true;
    }
}
