package com.yahoo.platform.yui.compressor;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

public class ResourceFileVisitor implements FileVisitor<Path> {
    private final DirectoryStream.Filter<Path> filter;
    private final ArrayList<Path> result;

    public ResourceFileVisitor(String type, ArrayList<Path> result) {

        FileSystem fs = FileSystems.getDefault();
        final PathMatcher matcher = fs.getPathMatcher("glob:" + "*." + type);
        DirectoryStream.Filter<Path> filter = entry -> matcher.matches(entry.getFileName());

        this.filter = filter;
        this.result = result;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (filter.accept(file)) {
            result.add(file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }
}
