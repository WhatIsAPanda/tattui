package app.entity;
import app.loader.ObjLoader;
public record ModelHandle(ObjLoader.LoadedModel loaded, String name) {}
