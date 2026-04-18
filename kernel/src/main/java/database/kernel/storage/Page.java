package database.kernel.storage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Page implements a slotted page architecture for a fixed-size (4KB) disk block.
 *
 * Layout:
 * ┌────────────────────────────────────────────────────┐
 * │ Header: pageId(4) tupleCount(4) freeSpaceEnd(4)    │
 * │ Slot Directory: [offset(4) + length(4)] × N        │
 * │ ... Free Space ...                                  │
 * │ ... Tuple Data (grows from the end) ←               │
 * └────────────────────────────────────────────────────┘
 *
 * Tuples are written from the END of the page backward.
 * Slot directory grows from the START (after the header) forward.
 * freeSpaceEnd points to the first byte of the last inserted tuple.
 *
 * A deleted slot has offset = -1.
 */
public class Page {

    public static final int PAGE_SIZE = 4096;
    private static final int HEADER_SIZE = 12; // pageId(4) + tupleCount(4) + freeSpaceEnd(4)
    private static final int SLOT_ENTRY_SIZE = 8; // offset(4) + length(4)
    private static final int DELETED_MARKER = -1;

    private int pageId;
    private int tupleCount; // number of slots (including deleted)
    private int freeSpaceEnd; // offset of the start of written tuple data
    private final byte[] data;

    /**
     * Create a new empty page.
     */
    public Page(int pageId) {
        this.pageId = pageId;
        this.tupleCount = 0;
        this.freeSpaceEnd = PAGE_SIZE;
        this.data = new byte[PAGE_SIZE];
    }

    /**
     * Private constructor for deserialization.
     */
    private Page(int pageId, int tupleCount, int freeSpaceEnd, byte[] data) {
        this.pageId = pageId;
        this.tupleCount = tupleCount;
        this.freeSpaceEnd = freeSpaceEnd;
        this.data = data;
    }

    public int getPageId() {
        return pageId;
    }

    public int getTupleCount() {
        return tupleCount;
    }

    /**
     * Returns the number of active (non-deleted) tuples on this page.
     */
    public int getActiveTupleCount() {
        int count = 0;
        for (int i = 0; i < tupleCount; i++) {
            if (getSlotOffset(i) != DELETED_MARKER) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the available free space for new tuples (in bytes).
     */
    public int getFreeSpace() {
        int slotDirEnd = HEADER_SIZE + tupleCount * SLOT_ENTRY_SIZE;
        return freeSpaceEnd - slotDirEnd - SLOT_ENTRY_SIZE; // reserve for new slot entry
    }

    /**
     * Insert a serialized tuple into this page.
     *
     * @param tupleData serialized tuple bytes
     * @return slot index, or -1 if not enough space
     */
    public int insertTuple(byte[] tupleData) {
        int needed = tupleData.length;
        if (getFreeSpace() < needed) {
            return -1;
        }

        // Write tuple data from end of page backward
        freeSpaceEnd -= needed;
        System.arraycopy(tupleData, 0, data, freeSpaceEnd, needed);

        // Add slot entry
        int slotId = tupleCount;
        setSlotEntry(slotId, freeSpaceEnd, needed);
        tupleCount++;

        return slotId;
    }

    /**
     * Retrieve the serialized tuple at the given slot.
     *
     * @param slotId slot index
     * @return tuple bytes, or null if slot is deleted
     */
    public byte[] getTuple(int slotId) {
        if (slotId < 0 || slotId >= tupleCount) {
            throw new IndexOutOfBoundsException("Invalid slot: " + slotId);
        }

        int offset = getSlotOffset(slotId);
        if (offset == DELETED_MARKER) {
            return null; // deleted
        }

        int length = getSlotLength(slotId);
        byte[] result = new byte[length];
        System.arraycopy(data, offset, result, 0, length);
        return result;
    }

    /**
     * Delete the tuple at the given slot (tombstone — marks as deleted).
     */
    public void deleteTuple(int slotId) {
        if (slotId < 0 || slotId >= tupleCount) {
            throw new IndexOutOfBoundsException("Invalid slot: " + slotId);
        }
        setSlotEntry(slotId, DELETED_MARKER, 0);
    }

    /**
     * Check if a slot is deleted.
     */
    public boolean isDeleted(int slotId) {
        if (slotId < 0 || slotId >= tupleCount) {
            throw new IndexOutOfBoundsException("Invalid slot: " + slotId);
        }
        return getSlotOffset(slotId) == DELETED_MARKER;
    }

    /**
     * Get all active (non-deleted) slot IDs on this page.
     */
    public List<Integer> getActiveSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < tupleCount; i++) {
            if (getSlotOffset(i) != DELETED_MARKER) {
                slots.add(i);
            }
        }
        return slots;
    }

    /**
     * Serialize the entire page to a 4096-byte array for disk writing.
     */
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.putInt(0, pageId);
        buf.putInt(4, tupleCount);
        buf.putInt(8, freeSpaceEnd);
        // Slot entries and tuple data are already in `data`
        return data.clone();
    }

    /**
     * Deserialize a 4096-byte array into a Page object.
     */
    public static Page deserialize(byte[] raw) {
        if (raw.length != PAGE_SIZE) {
            throw new IllegalArgumentException("Page data must be " + PAGE_SIZE + " bytes");
        }
        ByteBuffer buf = ByteBuffer.wrap(raw);
        int pageId = buf.getInt(0);
        int tupleCount = buf.getInt(4);
        int freeSpaceEnd = buf.getInt(8);
        return new Page(pageId, tupleCount, freeSpaceEnd, raw.clone());
    }

    // ---- Internal helpers ----

    private int slotDirPosition(int slotId) {
        return HEADER_SIZE + slotId * SLOT_ENTRY_SIZE;
    }

    private int getSlotOffset(int slotId) {
        int pos = slotDirPosition(slotId);
        return ByteBuffer.wrap(data, pos, 4).getInt();
    }

    private int getSlotLength(int slotId) {
        int pos = slotDirPosition(slotId) + 4;
        return ByteBuffer.wrap(data, pos, 4).getInt();
    }

    private void setSlotEntry(int slotId, int offset, int length) {
        int pos = slotDirPosition(slotId);
        ByteBuffer buf = ByteBuffer.wrap(data, pos, SLOT_ENTRY_SIZE);
        buf.putInt(offset);
        buf.putInt(length);
    }
}
