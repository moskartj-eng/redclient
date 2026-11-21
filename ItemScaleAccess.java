package de.redclient.mixin;

/** Wird in ItemEntityRenderState eingemischt, um den Groessenfaktor vom Update zum Render zu tragen. */
public interface ItemScaleAccess {
    float redclient$getScale();

    void redclient$setScale(float scale);
}
