package database.kernel.storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * HeapFile represents a table stored as multiple pages on disk.
 * Uses BufferPool for page caching instead of direct disk I/O.
 */
public class HeapFile {

    private final String tableName;
    private final Schema schema;
    private final BufferPool bufferPool;
    private int pageCount;

    public HeapFile(String tableName, Schema schema, BufferPool bufferPool) {
        this.tableName = tableName;
        this.schema = schema;
        this.bufferPool = bufferPool;
        this.pageCount = bufferPool.getPageCount(getFileName());
    }

    public String getTableName() {
        return tableName;
    }

    public String getFileName() {
        return tableName + ".dat";
    }

    public Schema getSchema() {
        return schema;
    }

    public int getPageCount() {
        return pageCount;
    }

    /**
     * Insert a tuple into the heap file.
     * Finds the first page with enough free space, or allocates a new page.
     *
     * @return RecordId pointing to the inserted tuple
     */
    public RecordId insertTuple(Tuple tuple) {
        byte[] tupleData = tuple.serialize();

        // Try to find an existing page with space
        for (int i = 0; i < pageCount; i++) {
            Page page = bufferPool.getPage(getFileName(), i);
            int slotId = page.insertTuple(tupleData);
            if (slotId != -1) {
                bufferPool.markDirty(getFileName(), i);
                return new RecordId(i, slotId);
            }
        }

        // No page has space — allocate a new one
        int newPageId = bufferPool.allocatePage(getFileName());
        pageCount = newPageId + 1;
        Page newPage = bufferPool.getPage(getFileName(), newPageId);
        int slotId = newPage.insertTuple(tupleData);
        if (slotId == -1) {
            throw new RuntimeException("Tuple too large for a single page");
        }
        bufferPool.markDirty(getFileName(), newPageId);
        return new RecordId(newPageId, slotId);
    }

    /**
     * Delete the tuple at the given record location.
     */
    public void deleteTuple(RecordId rid) {
        Page page = bufferPool.getPage(getFileName(), rid.getPageId());
        page.deleteTuple(rid.getSlotId());
        bufferPool.markDirty(getFileName(), rid.getPageId());
    }

    /**
     * Fetch the tuple at the given record location.
     *
     * @return the Tuple, or null if deleted
     */
    public Tuple getTuple(RecordId rid) {
        Page page = bufferPool.getPage(getFileName(), rid.getPageId());
        byte[] data = page.getTuple(rid.getSlotId());
        if (data == null) {
            return null; // deleted
        }
        return Tuple.deserialize(data, schema);
    }

    /**
     * Returns an iterator over all active tuples in the heap file.
     * Scans pages sequentially (used by SeqScan).
     */
    public Iterator<TupleWithId> iterator() {
        return new HeapFileIterator();
    }

    /**
     * A tuple paired with its RecordId for scan operations.
     */
    public static class TupleWithId {
        private final RecordId recordId;
        private final Tuple tuple;

        public TupleWithId(RecordId recordId, Tuple tuple) {
            this.recordId = recordId;
            this.tuple = tuple;
        }

        public RecordId getRecordId() { return recordId; }
        public Tuple getTuple() { return tuple; }
    }

    /**
     * Iterator that scans all pages and all active slots.
     */
    private class HeapFileIterator implements Iterator<TupleWithId> {
        private int currentPage = 0;
        private List<Integer> currentSlots = new ArrayList<>();
        private int slotIndex = 0;

        HeapFileIterator() {
            advanceToNextActiveSlot();
        }

        @Override
        public boolean hasNext() {
            return currentPage < pageCount && slotIndex < currentSlots.size();
        }

        @Override
        public TupleWithId next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            int slotId = currentSlots.get(slotIndex);
            RecordId rid = new RecordId(currentPage, slotId);
            Page page = bufferPool.getPage(getFileName(), currentPage);
            byte[] data = page.getTuple(slotId);
            Tuple tuple = Tuple.deserialize(data, schema);

            slotIndex++;
            if (slotIndex >= currentSlots.size()) {
                currentPage++;
                slotIndex = 0;
                advanceToNextActiveSlot();
            }

            return new TupleWithId(rid, tuple);
        }

        private void advanceToNextActiveSlot() {
            while (currentPage < pageCount) {
                Page page = bufferPool.getPage(getFileName(), currentPage);
                currentSlots = page.getActiveSlots();
                if (!currentSlots.isEmpty()) {
                    slotIndex = 0;
                    return;
                }
                currentPage++;
            }
        }
    }
}
