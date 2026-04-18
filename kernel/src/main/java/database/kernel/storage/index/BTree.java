package database.kernel.storage.index;

import database.kernel.storage.RecordId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * B+ Tree — a balanced search tree for indexing.
 *
 * All data (keys + RecordIds) lives in leaf nodes, which are linked
 * for efficient range scans. Internal nodes store separator keys only.
 *
 * Supports insert, search, range search, and delete with rebalancing.
 * Handles duplicate keys by storing multiple RecordIds per key.
 *
 * @param <K> the key type
 */
public class BTree<K extends Comparable<K>> {

    private BTreeNode<K> root;
    private final int t; // minimum degree
    private final int maxKeys; // 2t - 1
    private final int minKeys; // t - 1

    public BTree(int t) {
        if (t < 2) {
            throw new IllegalArgumentException("Minimum degree must be >= 2");
        }
        this.t = t;
        this.maxKeys = 2 * t - 1;
        this.minKeys = t - 1;
        this.root = new BTreeNode<>(true);
    }

    public BTree() {
        this(3);
    }

    public BTreeNode<K> getRoot() {
        return root;
    }

    public int getMinDegree() {
        return t;
    }

    // ========================================================================
    // INSERT
    // ========================================================================

    /**
     * Insert a key-recordId pair into the B+ Tree.
     */
    public void insert(K key, RecordId rid) {
        // Handle duplicate keys: if key already exists in a leaf, add rid to its list
        BTreeNode<K> leaf = findLeaf(root, key);
        int idx = findExactKeyIndex(leaf, key);
        if (idx != -1) {
            leaf.getRecordIdsForKey(idx).add(rid);
            return;
        }

        // Normal insert
        if (root.getKeyCount() == maxKeys) {
            BTreeNode<K> newRoot = new BTreeNode<>(false);
            newRoot.getChildren().add(root);
            splitChild(newRoot, 0);
            root = newRoot;
        }
        insertNonFull(root, key, rid);
    }

    private void insertNonFull(BTreeNode<K> node, K key, RecordId rid) {
        if (node.isLeaf()) {
            int idx = node.findKeyIndex(key);
            node.getKeys().add(idx, key);
            List<RecordId> rids = new ArrayList<>();
            rids.add(rid);
            node.getRecordIds().add(idx, rids);
        } else {
            int idx = node.findKeyIndex(key);
            // For internal nodes with B+ tree: equal keys go right
            if (idx < node.getKeyCount() && key.compareTo(node.getKey(idx)) == 0) {
                idx++;
            }

            BTreeNode<K> child = node.getChild(idx);
            if (child.getKeyCount() == maxKeys) {
                splitChild(node, idx);
                if (key.compareTo(node.getKey(idx)) >= 0) {
                    idx++;
                }
            }
            insertNonFull(node.getChild(idx), key, rid);
        }
    }

    /**
     * Split the i-th child of `parent` which is full (maxKeys keys).
     */
    private void splitChild(BTreeNode<K> parent, int i) {
        BTreeNode<K> full = parent.getChild(i);
        BTreeNode<K> sibling = new BTreeNode<>(full.isLeaf());

        if (full.isLeaf()) {
            // B+ tree leaf split: copy separator up, keep all data in leaves
            K separator = full.getKey(t);

            for (int j = t; j < full.getKeyCount(); j++) {
                sibling.getKeys().add(full.getKey(j));
                sibling.getRecordIds().add(full.getRecordIds().get(j));
            }

            // Maintain leaf sibling chain
            sibling.setNext(full.getNext());
            full.setNext(sibling);

            // Trim left: keep [0..t-1]
            int origSize = full.getKeyCount();
            for (int j = origSize - 1; j >= t; j--) {
                full.getKeys().remove(j);
                full.getRecordIds().remove(j);
            }

            // Insert separator into parent
            parent.getKeys().add(i, separator);
            parent.getChildren().add(i + 1, sibling);
        } else {
            // Standard B-tree internal split
            int midIndex = t - 1;
            K midKey = full.getKey(midIndex);

            for (int j = t; j < full.getKeyCount(); j++) {
                sibling.getKeys().add(full.getKey(j));
            }
            for (int j = t; j < full.getChildren().size(); j++) {
                sibling.getChildren().add(full.getChild(j));
            }

            int origKeyCount = full.getKeyCount();
            for (int j = origKeyCount - 1; j >= midIndex; j--) {
                full.getKeys().remove(j);
            }
            int origChildCount = full.getChildren().size();
            for (int j = origChildCount - 1; j >= t; j--) {
                full.getChildren().remove(j);
            }

            parent.getKeys().add(i, midKey);
            parent.getChildren().add(i + 1, sibling);
        }
    }

    // ========================================================================
    // SEARCH
    // ========================================================================

    /**
     * Search for a key and return the first matching RecordId.
     */
    public RecordId search(K key) {
        List<RecordId> results = searchAll(key);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Search for a key and return ALL matching RecordIds (handles duplicates).
     */
    public List<RecordId> searchAll(K key) {
        BTreeNode<K> leaf = findLeaf(root, key);
        int idx = findExactKeyIndex(leaf, key);
        if (idx != -1) {
            return new ArrayList<>(leaf.getRecordIdsForKey(idx));
        }
        return Collections.emptyList();
    }

    // ========================================================================
    // RANGE SEARCH
    // ========================================================================

    /**
     * Find all RecordIds for keys in [low, high] inclusive.
     */
    public List<RecordId> rangeSearch(K low, K high) {
        List<RecordId> results = new ArrayList<>();
        BTreeNode<K> leaf = findLeafForRange(root, low);

        while (leaf != null) {
            for (int i = 0; i < leaf.getKeyCount(); i++) {
                K k = leaf.getKey(i);
                if (k.compareTo(high) > 0) {
                    return results;
                }
                if (k.compareTo(low) >= 0) {
                    results.addAll(leaf.getRecordIdsForKey(i));
                }
            }
            leaf = leaf.getNext();
        }
        return results;
    }

    private BTreeNode<K> findLeaf(BTreeNode<K> node, K key) {
        if (node.isLeaf()) {
            return node;
        }
        int idx = node.findKeyIndex(key);
        if (idx < node.getKeyCount() && key.compareTo(node.getKey(idx)) == 0) {
            return findLeaf(node.getChild(idx + 1), key);
        }
        return findLeaf(node.getChild(idx), key);
    }

    private BTreeNode<K> findLeafForRange(BTreeNode<K> node, K key) {
        if (node.isLeaf()) {
            return node;
        }
        int idx = node.findKeyIndex(key);
        return findLeafForRange(node.getChild(idx), key);
    }

    private int findExactKeyIndex(BTreeNode<K> leaf, K key) {
        for (int i = 0; i < leaf.getKeyCount(); i++) {
            int cmp = key.compareTo(leaf.getKey(i));
            if (cmp == 0) return i;
            if (cmp < 0) return -1;
        }
        return -1;
    }

    // ========================================================================
    // DELETE
    // ========================================================================

    /**
     * Delete a key from the B+ Tree.
     */
    public boolean delete(K key) {
        if (root.getKeyCount() == 0) {
            return false;
        }

        boolean result = deleteFromNode(root, key);

        if (root.getKeyCount() == 0 && !root.isLeaf()) {
            root = root.getChild(0);
        }

        return result;
    }

    private boolean deleteFromNode(BTreeNode<K> node, K key) {
        int idx = node.findKeyIndex(key);

        if (node.isLeaf()) {
            if (idx < node.getKeyCount() && key.compareTo(node.getKey(idx)) == 0) {
                node.getKeys().remove(idx);
                node.getRecordIds().remove(idx);
                return true;
            }
            return false;
        }

        if (idx < node.getKeyCount() && key.compareTo(node.getKey(idx)) == 0) {
            BTreeNode<K> rightChild = node.getChild(idx + 1);
            if (rightChild.getKeyCount() < t) {
                fillChild(node, idx + 1);
                return deleteFromNode(node, key);
            }
            boolean result = deleteFromNode(rightChild, key);
            if (result && idx < node.getKeyCount()) {
                K newSeparator = getSuccessor(node.getChild(idx + 1));
                node.getKeys().set(idx, newSeparator);
            }
            return result;
        } else {
            BTreeNode<K> child = node.getChild(idx);
            if (child.getKeyCount() < t) {
                fillChild(node, idx);
                return deleteFromNode(node, key);
            }
            return deleteFromNode(child, key);
        }
    }

    private void fillChild(BTreeNode<K> node, int idx) {
        if (idx > 0 && node.getChild(idx - 1).getKeyCount() >= t) {
            borrowFromPrev(node, idx);
        } else if (idx < node.getChildren().size() - 1 && node.getChild(idx + 1).getKeyCount() >= t) {
            borrowFromNext(node, idx);
        } else {
            if (idx < node.getChildren().size() - 1) {
                mergeChildren(node, idx);
            } else {
                mergeChildren(node, idx - 1);
            }
        }
    }

    private void borrowFromPrev(BTreeNode<K> node, int idx) {
        BTreeNode<K> child = node.getChild(idx);
        BTreeNode<K> leftSibling = node.getChild(idx - 1);

        if (child.isLeaf()) {
            int lastIdx = leftSibling.getKeyCount() - 1;
            child.getKeys().add(0, leftSibling.getKeys().remove(lastIdx));
            child.getRecordIds().add(0, leftSibling.getRecordIds().remove(lastIdx));
            node.getKeys().set(idx - 1, child.getKey(0));
        } else {
            child.getKeys().add(0, node.getKey(idx - 1));
            child.getChildren().add(0,
                    leftSibling.getChildren().remove(leftSibling.getChildren().size() - 1));
            node.getKeys().set(idx - 1,
                    leftSibling.getKeys().remove(leftSibling.getKeyCount() - 1));
        }
    }

    private void borrowFromNext(BTreeNode<K> node, int idx) {
        BTreeNode<K> child = node.getChild(idx);
        BTreeNode<K> rightSibling = node.getChild(idx + 1);

        if (child.isLeaf()) {
            child.getKeys().add(rightSibling.getKeys().remove(0));
            child.getRecordIds().add(rightSibling.getRecordIds().remove(0));
            node.getKeys().set(idx, rightSibling.getKey(0));
        } else {
            child.getKeys().add(node.getKey(idx));
            child.getChildren().add(rightSibling.getChildren().remove(0));
            node.getKeys().set(idx, rightSibling.getKeys().remove(0));
        }
    }

    private void mergeChildren(BTreeNode<K> node, int idx) {
        BTreeNode<K> left = node.getChild(idx);
        BTreeNode<K> right = node.getChild(idx + 1);

        if (left.isLeaf()) {
            left.getKeys().addAll(right.getKeys());
            left.getRecordIds().addAll(right.getRecordIds());
            left.setNext(right.getNext());
        } else {
            left.getKeys().add(node.getKey(idx));
            left.getKeys().addAll(right.getKeys());
            left.getChildren().addAll(right.getChildren());
        }

        node.getKeys().remove(idx);
        node.getChildren().remove(idx + 1);
    }

    @SuppressWarnings("unused")
    private K getPredecessor(BTreeNode<K> node) {
        while (!node.isLeaf()) {
            node = node.getChild(node.getChildren().size() - 1);
        }
        return node.getKey(node.getKeyCount() - 1);
    }

    private K getSuccessor(BTreeNode<K> node) {
        while (!node.isLeaf()) {
            node = node.getChild(0);
        }
        return node.getKey(0);
    }

    // ========================================================================
    // TRAVERSAL & UTILITY
    // ========================================================================

    /**
     * In-order traversal — returns all keys sorted.
     */
    public List<K> inOrderTraversal() {
        List<K> result = new ArrayList<>();
        BTreeNode<K> leaf = getLeftmostLeaf();
        while (leaf != null) {
            result.addAll(leaf.getKeys());
            leaf = leaf.getNext();
        }
        return result;
    }

    private BTreeNode<K> getLeftmostLeaf() {
        BTreeNode<K> node = root;
        while (!node.isLeaf()) {
            node = node.getChild(0);
        }
        return node;
    }

    public int getHeight() {
        int h = 0;
        BTreeNode<K> cur = root;
        while (!cur.isLeaf()) {
            h++;
            cur = cur.getChild(0);
        }
        return h;
    }

    public boolean isBalanced() {
        return checkBalance(root, 0, getHeight());
    }

    private boolean checkBalance(BTreeNode<K> node, int depth, int expectedHeight) {
        if (node.isLeaf()) {
            return depth == expectedHeight;
        }
        for (int i = 0; i < node.getChildren().size(); i++) {
            if (!checkBalance(node.getChild(i), depth + 1, expectedHeight)) {
                return false;
            }
        }
        return true;
    }

    public boolean isEmpty() {
        return root.getKeyCount() == 0;
    }

    public int size() {
        return inOrderTraversal().size();
    }
}
