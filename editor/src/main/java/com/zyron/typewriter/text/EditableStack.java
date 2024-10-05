package com.zyron.typewriter.text;

import java.util.LinkedList;

class EditableStack {
    private static final int MAX_STACK_SIZE = 1000; // Limit to 200 actions
    
    private boolean isBatchEdit;
    /* for grouping batch operations */
    private int groupId;
    /* where new entries should go */
    private int top;
    /* timestamp for the previous edit operation */
    private long lastEditTime;

    private LinkedList<Action> stack = new LinkedList<>();

    private Editable editable; // Field to store the Editable reference

    public EditableStack(Editable editable) { // Constructor to accept Editable
        this.editable = editable;
    }

    /**
     * Undo the previous insert/delete operation
     * 
     * @return The suggested position of the caret after the undo, or -1 if
     *          there is nothing to undo
     */
    public int onUndo() {
        if (isUndo()) {
            Action lastUndo = stack.get(top - 1);
            int group = lastUndo.group;
            do {
                Action action = stack.get(top - 1);
                if (action.group != group) {
                    break;
                }

                lastUndo = action;
                action.onUndo();
                top--;
            } while(isUndo());
            return lastUndo.findUndoPosition();
        }
        return -1;
    }

    /**
     * Redo the previous insert/delete operation
     * 
     * @return The suggested position of the caret after the redo, or -1 if
     *          there is nothing to redo
     */
    public int onRedo() {
        if (isRedo()) {
            Action lastRedo = stack.get(top);
            int group = lastRedo.group;
            do {
                Action action = stack.get(top);
                if (action.group != group) {
                    break;
                }

                lastRedo = action;
                action.onRedo();
                top++;
            } while(isRedo());

            return lastRedo.findRedoPosition();
        }
        return -1;
    }

    /** 
     * extract common parts of captureInsert and captureDelete
     *
     * Records an insert operation. Should be called before the insertion is
     * actually done.
     */
    public void captureInsert(int start, int end, long time) {
        boolean mergeSuccess = false;

        if (isUndo()) {
            Action action = stack.get(top - 1);

            if (action instanceof InsertAction
                && action.merge(start, end, time)) {
                mergeSuccess = true;
            } else {
                action.recordData();
            }
        }

        if (!mergeSuccess) {
            push(new InsertAction(start, end, groupId));

            if (!isBatchEdit) {
                groupId++;
            }
        }
        lastEditTime = time;
    }

    /**
     * Records a delete operation. Should be called before the deletion is
     * actually done.
     */
    public void captureDelete(int start, int end, long time) {
        boolean mergeSuccess = false;

        if (isUndo()) {
            Action action = stack.get(top - 1);

            if (action instanceof DeleteAction
                && action.merge(start, end, time)) {
                mergeSuccess = true;
            } else {
                action.recordData();
            }
        }

        if (!mergeSuccess) {
            push(new DeleteAction(start, end, groupId));

            if (!isBatchEdit) {
                groupId++;
            }
        }
        lastEditTime = time;
    }
    
  // For Unlimited Undo Redo
  /*private void push(Action action) {
        trimStack();
        top++;
        stack.add(action);
    }*/
    
    private void push(Action action) {
        trimStack();
        if (stack.size() >= MAX_STACK_SIZE) {
            stack.removeFirst(); // Remove the oldest action to maintain the limit
            top--; // Adjust the top pointer accordingly
        }
        top++;
        stack.add(action);
    }
    
    private void trimStack() {
        while (stack.size() > top) {
            stack.removeLast();
        }
    }

    public final boolean isUndo() {
        return top > 0;
    }

    public final boolean isRedo() {
        return top < stack.size();
    }

    public boolean isBatchEdit() {
        return isBatchEdit;
    }

    public void beginBatchEdit() {
        isBatchEdit = true;
    }

    public void endBatchEdit() {
        isBatchEdit = false;
        groupId++;
    }

    private abstract class Action {
        /* Start position of the edit */
        public int start;
        /* End position of the edit */
        public int end;
        /* Contents of the affected segment */
        public String data;
        /* Group ID. Commands of the same group are undo/redo as a unit */
        public int group;
        /* 750ms in nanoseconds */
        public final long MERGE_TIME = 1000000000; 

        public abstract void onUndo();
        public abstract void onRedo();
        /* Populates data with the affected text */
        public abstract void recordData();
        public abstract int findUndoPosition();
        public abstract int findRedoPosition();

        /**
         * Attempts to merge in an edit. This will only be successful if the new
         * edit is continuous. See {@link UndoStack} for the requirements
         * of a continuous edit.
         * 
         * @param start Start position of the new edit
         * @param length Length of the newly edited segment
         * @param time Timestamp when the new edit was made. There are no 
         * restrictions  on the units used, as long as it is consistently used 
         * in the whole program
         * 
         * @return Whether the merge was successful
         */
        public abstract boolean merge(int start, int end, long time);
    }

    private class InsertAction extends Action {
        /**
         * Corresponds to an insertion of text of size length just before
         * start position.
         */
        public InsertAction(int start, int end, int group) {
            this.start = start;
            this.end = end;
            this.group = group;
        }

        @Override
        public boolean merge(int start, int end, long time) {
            if (lastEditTime < 0) {
                return false;
            }

            if ((time - lastEditTime) < MERGE_TIME
                && start == end) {
                end += end - start;
                trimStack();
                return true;
            }
            return false;
        }

        @Override
        public void recordData() {
            //TODO handle memory allocation failure
            data = editable.substring(start, end);
        }

        @Override
        public void onUndo() {
            if (data == null) {
                recordData();
                editable.shiftEditableStart(-(end - start));
            } else {
                //dummy timestamp of 0
                editable.delete(start, end, false, 0);
            }
        }

        @Override
        public void onRedo() {
            //dummy timestamp of 0
            editable.insert(start, data, false, 0);
        }

        @Override
        public int findRedoPosition() {
            return end;
        }

        @Override
        public int findUndoPosition() {
            return start;
        }
    }


    private class DeleteAction extends Action {
        /**
         * Corresponds to an deletion of text of size length starting from
         * start position, inclusive.
         */
        public DeleteAction(int start, int end, int group) {
            this.start = start;
            this.end = end;
            this.group = group;
        }

        @Override
        public boolean merge(int start, int end, long time) {
            if (lastEditTime < 0) {
                return false;
            }

            if ((time - lastEditTime) < MERGE_TIME
                && end == start) {
                start = start;
                trimStack();
                return true;
            }
            return false;
        }

        @Override
        public void recordData() {
            //TODO handle memory allocation failure
            data = new String(editable.editableSubSequence(end - start));
        }

        @Override
        public void onUndo() {
            if (data == null) {
                recordData();
                editable.shiftEditableStart(end - start);
            } else {
                //dummy timestamp of 0
                editable.insert(start, data, false, 0);
            }
        }

        @Override
        public void onRedo() {
            //dummy timestamp of 0
            editable.delete(start, end, false, 0);
        }

        @Override
        public int findRedoPosition() {
            return start;
        }

        @Override
        public int findUndoPosition() {
            return end;
        }
    }
}