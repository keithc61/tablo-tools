package tablo;

public interface MediaSelector {

	boolean isSelectedEpisode(String episode);

	boolean isSelectedSeason(String season);

	boolean isSelectedTitle(String title);

}
