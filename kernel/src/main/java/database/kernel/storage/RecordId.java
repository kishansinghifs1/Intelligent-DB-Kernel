package database.kernel.storage;

import java.util.Objects;

/**
 * RecordId is a pointer to a specific tuple on disk.
 * It identifies a tuple by its page number and slot position within that page.
 */
public class RecordId {

    private final int pageId;
    private final int slotId;

    public RecordId(int pageId, int slotId) {
        this.pageId = pageId;
        this.slotId = slotId;
    }

    public int getPageId() {
        return pageId;
    }

    public int getSlotId() {
        return slotId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordId recordId = (RecordId) o;
        return pageId == recordId.pageId && slotId == recordId.slotId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId, slotId);
    }

    @Override
    public String toString() {
        return "RecordId(page=" + pageId + ", slot=" + slotId + ")";
    }
}
