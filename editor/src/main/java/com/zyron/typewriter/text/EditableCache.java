package com.zyron.typewriter.text;

class EditableCache {
    
    private final int CACHE_SIZE = 10; 
    private EditablePair<Integer, Integer>[] cache; 

    @SuppressWarnings("unchecked")
    public EditableCache() {
        cache = new EditablePair[CACHE_SIZE];
        cache[0] = new EditablePair<Integer, Integer>(0, 0);
        for (int i = 1; i < CACHE_SIZE; ++i) {
            cache[i] = new EditablePair<Integer, Integer>(-1, -1);
        }
    }

    public EditablePair<Integer, Integer> getNearestLine(int lineIndex) {
        int nearestMatch = 0;
        int nearestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < CACHE_SIZE; ++i) {
            int distance = Math.abs(lineIndex - cache[i].first);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestMatch = i;
            }
        }

        EditablePair<Integer, Integer> nearestEntry = cache[nearestMatch];
        makeHead(nearestMatch);
        return nearestEntry;
    }

    public EditablePair<Integer, Integer> getNearestCharOffset(int charOffset) {
        int nearestMatch = 0;
        int nearestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < CACHE_SIZE; ++i) {
            int distance = Math.abs(charOffset - cache[i].second);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestMatch = i;
            }
        }

        EditablePair<Integer, Integer> nearestEntry = cache[nearestMatch];
        makeHead(nearestMatch);
        return nearestEntry;
    }

    private void makeHead(int newHead) {
        if (newHead == 0) {
            return; 
        }

        EditablePair<Integer, Integer> temp = cache[newHead];
        for (int i = newHead; i > 1; --i) {
            cache[i] = cache[i - 1];
        }
        cache[1] = temp; 
    }

    public void updateEntry(int lineIndex, int charOffset) {
        if (lineIndex <= 0) {
            return;
        }

        if (!replaceEntry(lineIndex, charOffset)) {
            insertEntry(lineIndex, charOffset);
        }
    }

    private boolean replaceEntry(int lineIndex, int charOffset) {
        for (int i = 1; i < CACHE_SIZE; ++i) {
            if (cache[i].first == lineIndex) {
                cache[i].second = charOffset;
                return true;
            }
        }
        return false;
    }

    private void insertEntry(int lineIndex, int charOffset) {
        makeHead(CACHE_SIZE - 1); 
        cache[1] = new EditablePair<Integer, Integer>(lineIndex, charOffset);
    }

    public void invalidateCache(int fromCharOffset) {
        for (int i = 1; i < CACHE_SIZE; ++i) {
            if (cache[i].second >= fromCharOffset) {
                cache[i] = new EditablePair<Integer, Integer>(-1, -1);
            }
        }
    }
}