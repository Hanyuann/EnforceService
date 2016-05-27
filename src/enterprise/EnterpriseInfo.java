package enterprise;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import utils.Neo4jJdbcUtils;

@Produces("application/json")
@Consumes("application/json")
@Path("enterprise")
public class EnterpriseInfo {
	private final int PAGE_SIZE = 10;

	private static final int MODE_SUPPLY_FROM = 1;
	private static final int MODE_SUPPLY_TO = 2;
	private static final int MODE_FOLLOWER = 3;
	private static final int MODE_HAS_FOLLOW = 4;

	@POST
	@Produces("application/json")
	@Consumes("application/x-www-form-urlencoded")
	@Path("login")
	public String login(@FormParam("mc") String mc) {
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			conn = Neo4jJdbcUtils.getConnection();
			st = conn.createStatement();
			rs = st.executeQuery("match (e:Enterprise) where e.mc=\"" + mc
					+ "\" return e");
			if (rs.next()) {
				return rs.getString(1);
			} else {
				return "{}";
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Neo4jJdbcUtils.release(conn, st, rs);
		}
		return "{}";
	}

	@GET
	@Produces("application/json")
	@Path("getPartnersByEnterpriseMc")
	// 根据企业 ID，查找其所有业务企业
	public String getPartnersByEnterpriseMc(@QueryParam("mc") String mc,
			@QueryParam("page") int page, @QueryParam("mode") int mode) {
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;

		try {
			StringBuffer sb = new StringBuffer("{");
			conn = Neo4jJdbcUtils.getConnection();
			st = conn.createStatement();

			switch (mode) {
			case MODE_SUPPLY_FROM:
				rs = st.executeQuery("match (e:Enterprise)<-[r:Supply_to]-(p:Enterprise) where e.mc=\""
						+ mc + "\" and r.status=\"NORMAL\" return count(p)");
				break;
			case MODE_SUPPLY_TO:
				rs = st.executeQuery("match (e:Enterprise)-[r:Supply_to]->(p:Enterprise) where e.mc=\""
						+ mc + "\" and r.status=\"NORMAL\" return count(p)");
				break;
			case MODE_FOLLOWER:
				rs = st.executeQuery("match (e:Enterprise)<-[r:HasFollow]-(p:Enterprise) where e.mc=\""
						+ mc + "\" and r.status=\"NORMAL\" return count(p)");
				break;
			case MODE_HAS_FOLLOW:
				rs = st.executeQuery("match (e:Enterprise)-[r:Has_follow]->(p:Enterprise) where e.mc=\""
						+ mc + "\" and r.status=\"NORMAL\" return count(p)");
				break;
			}
			
			if (rs.next()) {
				sb.append("\"total\":" + rs.getString(1) + ",");
				if (rs.getString(1).equals("0")) {
					return "{}";
				}
			} else {
				return "{}";
			}
			rs.close();
			
			switch (mode) {
			case MODE_SUPPLY_FROM:
				rs = st.executeQuery("match (e:Enterprise)<-[r:Supply_to]-(p:Enterprise) where e.mc=\""
						+ mc
						+ "\" and r.status=\"NORMAL\" return p skip "
						+ PAGE_SIZE * (page - 1) + " limit " + PAGE_SIZE);
				break;
			case MODE_SUPPLY_TO:
				rs = st.executeQuery("match (e:Enterprise)-[r:Supply_to]->(p:Enterprise) where e.mc=\""
						+ mc
						+ "\" and r.status=\"NORMAL\" return p skip "
						+ PAGE_SIZE * (page - 1) + " limit " + PAGE_SIZE);
				break;
			case MODE_FOLLOWER:
				rs = st.executeQuery("match (e:Enterprise)<-[r:Has_follow]-(p:Enterprise) where e.mc=\""
						+ mc
						+ "\" and r.status=\"NORMAL\" return p skip "
						+ PAGE_SIZE * (page - 1) + " limit " + PAGE_SIZE);
				break;
			case MODE_HAS_FOLLOW:
				rs = st.executeQuery("match (e:Enterprise)-[r:Has_follow]->(p:Enterprise) where e.mc=\""
						+ mc
						+ "\" and r.status=\"NORMAL\" return p skip "
						+ PAGE_SIZE * (page - 1) + " limit " + PAGE_SIZE);
				break;
			}
			
			sb.append("\"Enterprises\":[");
			while (rs.next()) {
				sb.append(rs.getString(1) + ",");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append("]}");
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Neo4jJdbcUtils.release(conn, st, rs);
		}
		return "{}";
	}
}
