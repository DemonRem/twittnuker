package de.vanita5.twittnuker.api.twitter.model;

public interface MediaUploadResponse extends TwitterResponse {

    long getId();

    Image getImage();

    long getSize();

    interface Image {

        int getHeight();

        String getImageType();

        int getWidth();
	}
}