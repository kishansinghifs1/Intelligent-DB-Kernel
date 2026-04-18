package database.kernel.storage.index;

import database.kernel.storage.RecordId;

import java.util.ArrayList;
import java.util.List;

/**
 * BTreeNode represents a single node in a B-Tree.
 * Can be either an internal node (has children) or a leaf node (has recordIds).
 *
 * @param <K> the key type, must be comparable
 */
public class BTreeNode<K extends Comparable<K>> {

    private boolean isLeaf;
    private final List<K> keys;
    private final List<BTreeNode<K>> children;      // internal nodes only
    private final List<List<RecordId>> recordIds;    // leaf nodes only (list per key for duplicates)
    private BTreeNode<K> next; // leaf sibling pointer for range scans

    public BTreeNode(boolean isLeaf) {
        this.isLeaf = isLeaf;
        this.keys = new ArrayList<>();
        this.children = new ArrayList<>();
        this.recordIds = new ArrayList<>();
        this.next = null;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
    }

    public List<K> getKeys() {
        return keys;
    }

    public int getKeyCount() {
        return keys.size();
    }

    public K getKey(int index) {
        return keys.get(index);
    }

    public List<BTreeNode<K>> getChildren() {
        return children;
    }

    public BTreeNode<K> getChild(int index) {
        return children.get(index);
    }

    public List<List<RecordId>> getRecordIds() {
        return recordIds;
    }

    public List<RecordId> getRecordIdsForKey(int index) {
        return recordIds.get(index);
    }

    public BTreeNode<K> getNext() {
        return next;
    }

    public void setNext(BTreeNode<K> next) {
        this.next = next;
    }

    /**
     * Find the index of the first key >= the given key using binary search.
     */
    public int findKeyIndex(K key) {
        int lo = 0, hi = keys.size() - 1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            int cmp = key.compareTo(keys.get(mid));
            if (cmp <= 0) {
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }
        return lo;
    }

    @Override
    public String toString() {
        return (isLeaf ? "Leaf" : "Internal") + keys.toString();
    }
}
