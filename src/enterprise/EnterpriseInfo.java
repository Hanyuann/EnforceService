package enterprise;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
				rs = st.executeQuery("match (e:Enterprise)<-[r:Has_follow]-(p:Enterprise) where e.mc=\""
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

	@SuppressWarnings("resource")
	@POST
	@Produces("application/json")
	@Consumes("application/x-www-form-urlencoded")
	@Path("sendRelationApply")
	// 根据本方企业 mc 和对方企业 mc 发出建立关系申请
	public String sendRelationApply(@FormParam("mc") String mc,
			@FormParam("mc2") String mc2, @FormParam("mode") int mode) {
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		boolean success = false;
		try {
			conn = Neo4jJdbcUtils.getConnection();
			st = conn.createStatement();
			rs = st.executeQuery("match (e:Enterprise) where e.mc=\"" + mc2
					+ "\"" + "return e");
			if (rs.next()) {
				rs.close();
				switch (mode) {
				case MODE_SUPPLY_FROM:
					rs = st.executeQuery("match (e:Enterprise)<-[r:Supply_to]-(p:Enterprise) where e.mc=\""
							+ mc2
							+ "\" and p.mc=\""
							+ mc
							+ "\" and r.status=\"INVALID\" return r");
					if (rs.next()) {
						return "{\"message\": \"send\"}";
					}
					rs = st.executeQuery("match (e:Enterprise)<-[r:Supply_to]-(p:Enterprise) where e.mc=\""
							+ mc2
							+ "\" and p.mc=\""
							+ mc
							+ "\" and r.status=\"NORMAL\" return r");
					if (rs.next()) {
						return "{\"message\": \"exist\"}";
					}
					success = st
							.execute("match (e:Enterprise) where e.mc=\""
									+ mc2
									+ "\" match (p:Enterprise) where p.mc=\""
									+ mc
									+ "\" create (e)<-[r:Supply_to]-(p) set r.status=\"INVALID\" return r");
					break;
				case MODE_SUPPLY_TO:
					rs = st.executeQuery("match (e:Enterprise)-[r:Supply_to]->(p:Enterprise) where e.mc=\""
							+ mc2
							+ "\" and p.mc=\""
							+ mc
							+ "\" and r.status=\"INVALID\" " + "return r");
					if (rs.next()) {
						return "{\"message\": \"send\"}";
					}
					rs = st.executeQuery("match (e:Enterprise)-[r:Supply_to]->(p:Enterprise) where e.mc=\""
							+ mc2
							+ "\" and p.mc=\""
							+ mc
							+ "\" and r.status=\"NORMAL\" " + "return r");
					if (rs.next()) {
						return "{\"message\": \"exist\"}";
					}
					success = st
							.execute("match (e:Enterprise) where e.mc=\""
									+ mc2
									+ "\" "
									+ "match (p:Enterprise) where p.mc=\""
									+ mc
									+ "\" create (e)-[r:Supply_to]->(p) set r.status=\"INVALID\" return r");
					break;
				case MODE_FOLLOWER:
					rs = st.executeQuery("match (e:Enterprise)<-[r:Has_follow]-(p:Enterprise) where e.mc=\""
							+ mc2
							+ "\" and p.mc=\""
							+ mc
							+ "\" and r.status=\"INVALID\" " + "return r");
					if (rs.next()) {
						return "{\"message\": \"send\"}";
					}
					rs = st.executeQuery("match (e:Enterprise)<-[r:Has_follow]-(p:Enterprise) where e.mc=\""
							+ mc2
							+ "\" and p.mc=\""
							+ mc
							+ "\" and r.status=\"NORMAL\" " + "return r");
					if (rs.next()) {
						return "{\"message\": \"exist\"}";
					}
					success = st
							.execute("match (e:Enterprise) where e.mc=\""
									+ mc2
									+ "\" match (p:Enterprise) where p.mc=\""
									+ mc
									+ "\" create (e)<-[r:Has_follow]-(p) set r.status=\"INVALID\" return r");
					break;
				case MODE_HAS_FOLLOW:
					rs = st.executeQuery("match (e:Enterprise)-[r:Has_follow]->(p:Enterprise) where e.mc=\""
							+ mc2
							+ "\" and p.mc=\""
							+ mc
							+ "\" and r.status=\"INVALID\" " + "return r");
					if (rs.next()) {
						return "{\"message\": \"send\"}";
					}
					rs = st.executeQuery("match (e:Enterprise)-[r:Has_follow]->(p:Enterprise) where e.mc=\""
							+ mc2
							+ "\" and p.mc=\""
							+ mc
							+ "\" and r.status=\"NORMAL\" " + "return r");
					if (rs.next()) {
						return "{\"message\": \"exist\"}";
					}
					success = st
							.execute("match (e:Enterprise) where e.mc=\""
									+ mc2
									+ "\" match (p:Enterprise) where p.mc=\""
									+ mc
									+ "\" create (e)-[r:Has_follow]->(p) set r.status=\"INVALID\" return r");
					break;
				}
				if (success) {
					return "{\"message\": \"ok\"}";
				}
			} else {
				return "{\"message\": \"no\"}";
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
	@Path("getReiationApply")
	// 查看所有期望建立关系的请求
	public String getRelationApply(@QueryParam("mc") String mc) {
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		boolean flag = false;

		try {
			StringBuffer sb = new StringBuffer("{");
			conn = Neo4jJdbcUtils.getConnection();
			st = conn.createStatement();

			sb.append("\"Enterprises_Supply_From\":[");
			rs = st.executeQuery("match (e:Enterprise)<-[r:Supply_to]-(p:Enterprise) where e.mc=\""
					+ mc + "\" and r.status=\"INVALID\" return p");
			while (rs.next()) {
				flag = true;
				sb.append(rs.getString(1) + ",");
			}
			if (flag) {
				sb.deleteCharAt(sb.length() - 1);
				sb.append("],");
				flag = false;
			} else {
				sb.delete(sb.length() - 27, sb.length());
			}
			rs.close();
			sb.append("\"Enterprises_Supply_To\":[");
			rs = st.executeQuery("match (e:Enterprise)-[r:Supply_to]->(p:Enterprise) where e.mc=\""
					+ mc + "\" and r.status=\"INVALID\" return p");
			while (rs.next()) {
				flag = true;
				sb.append(rs.getString(1) + ",");
			}
			if (flag) {
				sb.deleteCharAt(sb.length() - 1);
				sb.append("],");
				flag = false;
			} else {
				sb.delete(sb.length() - 25, sb.length());
			}
			rs.close();
			sb.append("\"Enterprises_Follower\":[");
			rs = st.executeQuery("match (e:Enterprise)<-[r:Has_follow]-(p:Enterprise) where e.mc=\""
					+ mc + "\" and r.status=\"INVALID\" return p");
			while (rs.next()) {
				flag = true;
				sb.append(rs.getString(1) + ",");
			}
			if (flag) {
				sb.deleteCharAt(sb.length() - 1);
				sb.append("],");
				flag = false;
			} else {
				sb.delete(sb.length() - 24, sb.length());
			}
			rs.close();
			sb.append("\"Enterprises_Has_Follow\":[");
			rs = st.executeQuery("match (e:Enterprise)-[r:Has_follow]->(p:Enterprise) where e.mc=\""
					+ mc + "\" and r.status=\"INVALID\" return p");
			while (rs.next()) {
				flag = true;
				sb.append(rs.getString(1) + ",");
			}
			if (flag) {
				sb.deleteCharAt(sb.length() - 1);
				sb.append("]}");
				flag = false;
			} else {
				sb.delete(sb.length() - 27, sb.length());
				if (sb.length() == 0) {
					sb.append("{");
				}
				sb.append("}");
			}
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Neo4jJdbcUtils.release(conn, st, rs);
		}
		return "{}";
	}

	@PUT
	@Produces("application/json")
	@Consumes("application/x-www-form-urlencoded")
	@Path("saveRelationApply")
	// 接收对方建立关系的请求
	public String saveRelationApply(@QueryParam("mc") String mc,
			@QueryParam("mc2") String mc2, @QueryParam("mode") int mode) {
		Connection conn = null;
		Statement st = null;
		boolean success = false;

		try {
			conn = Neo4jJdbcUtils.getConnection();
			st = conn.createStatement();
			switch (mode) {
			case MODE_SUPPLY_FROM:
				success = st
						.execute("match (e:Enterprise)<-[r:Supply_to]-(p:Enterprise) where e.mc=\""
								+ mc
								+ "\" and p.mc=\""
								+ mc2
								+ "\" set r.status=\"NORMAL\" return r");
				break;
			case MODE_SUPPLY_TO:
				success = st
						.execute("match (e:Enterprise)-[r:Supply_to]->(p:Enterprise) where e.mc=\""
								+ mc
								+ "\" and p.mc=\""
								+ mc2
								+ "\" set r.status=\"NORMAL\" return r");
				break;
			case MODE_FOLLOWER:
				success = st
						.execute("match (e:Enterprise)<-[r:Has_follow]-(p:Enterprise) where e.mc=\""
								+ mc
								+ "\" and p.mc=\""
								+ mc2
								+ "\" set r.status=\"NORMAL\" return r");
				break;
			case MODE_HAS_FOLLOW:
				success = st
						.execute("match (e:Enterprise)-[r:Has_follow]->(p:Enterprise) where e.mc=\""
								+ mc
								+ "\" and p.mc=\""
								+ mc2
								+ "\" set r.status=\"NORMAL\" return r");
				break;
			}
			if (success) {
				return "{\"message\": \"ok\"}";
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Neo4jJdbcUtils.release(conn, st, null);
		}
		return "{}";
	}

	@DELETE
	@Produces("application/json")
	@Consumes("application/json")
	@Path("deleteEnterpriseRelation")
	// 根据关系类型，取消与其他企业的关系
	public String deleteEnterpriseRelation(@QueryParam("mc") String mc,
			@QueryParam("mc2") String mc2, @QueryParam("mode") int mode) {
		Connection conn = null;
		Statement st = null;
		boolean success = false;
		try {
			conn = Neo4jJdbcUtils.getConnection();
			st = conn.createStatement();
			switch (mode) {
			case MODE_SUPPLY_FROM:
				success = st
						.execute("match (e:Enterprise)<-[r:Supply_to]-(p:Enterprise) where e.mc=\""
								+ mc + "\" and p.mc=\"" + mc2 + "\" delete r");
				break;
			case MODE_SUPPLY_TO:
				success = st
						.execute("match (e:Enterprise)-[r:Supply_to]->(p:Enterprise) where e.mc=\""
								+ mc + "\" and p.mc=\"" + mc2 + "\" delete r");
				break;
			case MODE_FOLLOWER:
				success = st
						.execute("match (e:Enterprise)<-[r:Has_follow]-(p:Enterprise) where e.mc=\""
								+ mc
								+ "\" and p.mc=\""
								+ mc2
								+ "\" "
								+ "delete r");
				break;
			case MODE_HAS_FOLLOW:
				success = st
						.execute("match (e:Enterprise)-[r:Has_follow]->(p:Enterprise) where e.mc=\""
								+ mc
								+ "\" and p.mc=\""
								+ mc2
								+ "\" "
								+ "delete r");
				break;
			}
			if (success) {
				return "{\"message\": \"ok\"}";
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Neo4jJdbcUtils.release(conn, st, null);
		}
		return "{}";
	}
}
