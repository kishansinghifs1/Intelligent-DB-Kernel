package database.kernel.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * DiskManager handles raw page I/O to disk files.
 * Each table is stored as a separate .dat file.
 *
 * Spring-managed component with configurable data directory.
 */
@Component
public class DiskManager {

    private static final Logger log = LoggerFactory.getLogger(DiskManager.class);

    @Value("${minipostgres.data-dir:kernel-data}")
    private String dataDirectory;

    // No-arg constructor for Spring
    public DiskManager() {}

    // Constructor for programmatic use (tests, etc.)
    public DiskManager(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        initDirectory();
    }

    @PostConstruct
    public void initDirectory() {
        File dir = new File(dataDirectory);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            log.info("Data directory '{}' {}", dataDirectory, created ? "created" : "already exists");
        }
    }

    /**
     * Read a page from disk.
     *
     * @param fileName the table file (e.g., "students.dat")
     * @param pageId   the page number
     * @return deserialized Page
     */
    public Page readPage(String fileName, int pageId) {
        File file = new File(dataDirectory, fileName);
        if (!file.exists()) {
            throw new RuntimeException("File not found: " + file.getAbsolutePath());
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long offset = (long) pageId * Page.PAGE_SIZE;
            if (offset >= raf.length()) {
                throw new RuntimeException("Page " + pageId + " out of range for " + fileName);
            }
            raf.seek(offset);
            byte[] data = new byte[Page.PAGE_SIZE];
            raf.readFully(data);
            return Page.deserialize(data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read page " + pageId + " from " + fileName, e);
        }
    }

    /**
     * Write a page to disk.
     */
    public void writePage(String fileName, int pageId, Page page) {
        File file = new File(dataDirectory, fileName);
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            long offset = (long) pageId * Page.PAGE_SIZE;
            raf.seek(offset);
            raf.write(page.serialize());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write page " + pageId + " to " + fileName, e);
        }
    }

    /**
     * Allocate a new empty page at the end of the file.
     *
     * @return the new page's ID
     */
    public int allocatePage(String fileName) {
        File file = new File(dataDirectory, fileName);
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            int newPageId = (int) (raf.length() / Page.PAGE_SIZE);
            Page emptyPage = new Page(newPageId);
            raf.seek((long) newPageId * Page.PAGE_SIZE);
            raf.write(emptyPage.serialize());
            return newPageId;
        } catch (IOException e) {
            throw new RuntimeException("Failed to allocate page in " + fileName, e);
        }
    }

    /**
     * Get the total number of pages in a file.
     */
    public int getPageCount(String fileName) {
        File file = new File(dataDirectory, fileName);
        if (!file.exists()) {
            return 0;
        }
        return (int) (file.length() / Page.PAGE_SIZE);
    }

    /**
     * Delete a data file (used for dropping tables).
     */
    public boolean deleteFile(String fileName) {
        File file = new File(dataDirectory, fileName);
        return file.delete();
    }

    public String getDataDirectory() {
        return dataDirectory;
    }
}
