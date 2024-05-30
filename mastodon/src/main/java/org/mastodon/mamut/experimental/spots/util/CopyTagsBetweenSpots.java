package org.mastodon.mamut.experimental.spots.util;

import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.tag.ObjTagMap;
import org.mastodon.model.tag.TagSetModel;
import org.mastodon.model.tag.TagSetStructure;
import org.mastodon.model.tag.ui.TagSetDialog;

public class CopyTagsBetweenSpots {
	private TagSetModel<Spot, Link> tagSetModel;

	public CopyTagsBetweenSpots(ProjectModel projectModel) {
		tagSetModel = projectModel.getModel().getTagSetModel();
	}


	public void deleteSpotFromAllTS(final Spot spot) {
		tagSetModel.getTagSetStructure().getTagSets().forEach(ts -> {
			ObjTagMap<Spot, TagSetStructure.Tag> tagMap = tagSetModel.getVertexTags().tags(ts);
			if (tagMap.get(spot) != null) { tagMap.remove(spot); }
		});
	}

	public void insertSpotIntoSameTSAs(final Spot spot, final Spot referenceSpot) {
		tagSetModel.getTagSetStructure().getTagSets().forEach(ts -> {
			ObjTagMap<Spot, TagSetStructure.Tag> tagMap = tagSetModel.getVertexTags().tags(ts);
			TagSetStructure.Tag tag = tagMap.get(referenceSpot);
			if (tag != null) { tagMap.set(spot,tag); }
		});
	}
}
