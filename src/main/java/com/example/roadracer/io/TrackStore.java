package com.example.roadracer.io;

import com.example.roadracer.model.Track;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TrackStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private TrackStore() {}

    public static void save(Track track, Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(path, GSON.toJson(track), StandardCharsets.UTF_8);
    }

    public static Track load(Path path) throws IOException {
        Track track = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Track.class);
        if (track == null || track.getPoints() == null) throw new IOException("Файл не содержит корректную трассу.");
        return track;
    }
}
