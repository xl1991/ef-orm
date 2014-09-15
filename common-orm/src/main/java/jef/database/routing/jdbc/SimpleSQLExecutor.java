package jef.database.routing.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;

import jef.database.OperateTarget;
import jef.database.wrapper.result.JdbcResultSetAdapter;
import jef.database.wrapper.result.ResultSetWrapper;

public class SimpleSQLExecutor implements SQLExecutor {
	private String sql;
	private OperateTarget db;
	private int fetchSize;
	private int maxRows;
	private int queryTimeout;
	
	public SimpleSQLExecutor(OperateTarget target, String sql) {
		this.db=target;
		this.sql=sql;
	}

	@Override
	public UpdateReturn executeUpdate(int generateKeys,int[] columnIndexs,String[] returnColumns,List<ParameterContext> params) throws SQLException {
		PreparedStatement st;
		boolean returnKeys=false;
		if(columnIndexs!=null){
			st=db.prepareStatement(sql, columnIndexs);
			returnKeys=true;
		}else if(returnColumns!=null){
			st=db.prepareStatement(sql, returnColumns);
			returnKeys=true;
		}else if(generateKeys==Statement.RETURN_GENERATED_KEYS){
			st=db.prepareStatement(sql, generateKeys);
			returnKeys=true;
		}else{
			st=db.prepareStatement(sql);
		}
		try{
			for(ParameterContext context:params){
				context.apply(st);
			}
			UpdateReturn result=new UpdateReturn(st.executeUpdate());
			if(returnKeys){
				result.cacheGeneratedKeys(st.getGeneratedKeys());
			}
			return result;
		}finally{
			st.close();
		}
	}

	@Override
	public ResultSet getResultSet(int type,int concurrency,int holder,List<ParameterContext> params) throws SQLException {
		if(type<1){
			type=ResultSet.TYPE_FORWARD_ONLY;
		}
		if(concurrency<1){
			concurrency=ResultSet.CONCUR_READ_ONLY;
		}
		if(holder<1){
			holder=ResultSet.CLOSE_CURSORS_AT_COMMIT;
		}
		PreparedStatement st=db.prepareStatement(sql,type,concurrency,holder);
		try{
			if(fetchSize>0)
				st.setFetchSize(fetchSize);
			if(maxRows>0);
				st.setMaxRows(maxRows);
			if(queryTimeout>0)
				st.setQueryTimeout(queryTimeout);
			for(ParameterContext context:params){
				context.apply(st);
			}
			ResultSet rs=st.executeQuery();
			return new JdbcResultSetAdapter(new ResultSetWrapper(db,st,rs));	
		}finally{
			st.close();
		}
	}
	

	@Override
	public void setFetchSize(int fetchSize) {
		this.fetchSize=fetchSize;
	}

	@Override
	public void setMaxResults(int maxRows) {
		this.maxRows=maxRows;
	}

	@Override
	public void setQueryTimeout(int queryTimeout) {
		this.queryTimeout=queryTimeout;
	}

	@Override
	public BatchReturn executeBatch(int autoGeneratedKeys, int[] columnIndexs, String[] columnNames, List<List<ParameterContext>> params) throws SQLException {
		PreparedStatement st;
		boolean returnKeys=false;
		if(columnIndexs!=null){
			st=db.prepareStatement(sql, columnIndexs);
			returnKeys=true;
		}else if(columnNames!=null){
			st=db.prepareStatement(sql, columnNames);
			returnKeys=true;
		}else if(autoGeneratedKeys==Statement.RETURN_GENERATED_KEYS){
			st=db.prepareStatement(sql, autoGeneratedKeys);
			returnKeys=true;
		}else{
			st=db.prepareStatement(sql);
		}
		for(Collection<ParameterContext> param:params){
			for(ParameterContext context:param){
				context.apply(st);
			}
			st.addBatch();
		}
		try{
			int[] re=st.executeBatch();
			BatchReturn result=new BatchReturn(re);
			if(returnKeys){
				result.cacheGeneratedKeys(st.getGeneratedKeys());
			}
			return result;
		}finally{
			st.close();
		}
	}
}
