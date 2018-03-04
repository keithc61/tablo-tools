package tablo;

public interface Configuration {

	String directoryFor(MediaType mediaType);

	boolean isSelectedEpisode(String episode);

	boolean isSelectedSeason(String season);

	boolean isSelectedTitle(String title);

}
