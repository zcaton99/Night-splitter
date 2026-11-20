package com.rdt.connectedfreedom;
import java.nio.file.*;
import java.io.IOException;
import com.google.gson.Gson;
final class WorldStore {
    static final Gson JSON=new Gson();
    static <T>T read(Path p,Class<T> type,T fallback){try{if(!Files.exists(p))return fallback;T value=JSON.fromJson(Files.readString(p),type);if(value==null)throw new IOException("Empty progress file");return value;}catch(Exception e){throw new IllegalStateException("Cannot read RDT world progress",e);}}
    static void write(Path p,Object value){try{if(p.getParent()!=null)Files.createDirectories(p.getParent());Path tmp=p.resolveSibling(p.getFileName()+".tmp");Files.writeString(tmp,JSON.toJson(value)+"\n");try{Files.move(tmp,p,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,p,StandardCopyOption.REPLACE_EXISTING);}}catch(IOException e){throw new IllegalStateException("Cannot save RDT world progress",e);}}
}
