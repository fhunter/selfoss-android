package fr.ydelouis.selfoss.model;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.List;

import fr.ydelouis.selfoss.entity.Article;
import fr.ydelouis.selfoss.entity.ArticleType;
import fr.ydelouis.selfoss.entity.Tag;

public class ArticleDao extends BaseDaoImpl<Article, Integer> {

	public static final String COLUMN_DATETIME = "dateTime";
	public static final String COLUMN_UNREAD = "unread";
	public static final String COLUMN_FAVORITE = "favorite";
	public static final String COLUMN_TAGS = "tags";

	public ArticleDao(ConnectionSource connectionSource) throws SQLException {
		super(connectionSource, Article.class);
	}

	@Override
	public CreateOrUpdateStatus createOrUpdate(Article data) {
		try {
			return super.createOrUpdate(data);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<Article> queryForNext(ArticleType type, Tag tag, Article article, long pageSize) {
		try {
			QueryBuilder<Article, Integer> queryBuilder = queryBuilder();
			Where<Article, Integer> where = queryBuilder.where();

			if (type == ArticleType.Unread) {
				where.eq(COLUMN_UNREAD, true);
			} else if (type == ArticleType.Favorite) {
				where.eq(COLUMN_FAVORITE, true);
			} else {
				where.gt(COLUMN_DATETIME, 0);
			}

			if (!Tag.ALL.equals(tag)) {
				where.and().like(COLUMN_TAGS, "%" + tag.getName(null) + "%");
			}

			if (article != null) {
				where.and().lt(COLUMN_DATETIME, article.getDateTime());
			}

			queryBuilder.orderBy(COLUMN_DATETIME, false);
			queryBuilder.offset(0l).limit(pageSize);

			return queryBuilder.query();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void removeOlderThan(long dateTime) {
		try {
			DeleteBuilder<Article, Integer> deleteBuilder = deleteBuilder();
			deleteBuilder.where().eq(COLUMN_FAVORITE, false)
				.and().eq(COLUMN_UNREAD, false)
				.and().lt(COLUMN_DATETIME, dateTime);
			deleteBuilder.delete();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

}
