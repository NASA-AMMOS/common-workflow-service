package jpl.cws.core.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/**
 * Wrapper around sl4j Logger that prepend CWS-specific tags
 *
 */
public abstract class CwsLogger implements Logger {

	protected final Logger log;
	private String tag;
	
	public CwsLogger(String clazzName) {
		log = LoggerFactory.getLogger(clazzName);
	}
	
	public CwsLogger(Class clazz) {
		log = LoggerFactory.getLogger(clazz);
	}
	
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}

	@Override
	public String getName() {
		return log.getName();
	}

	@Override
	public boolean isTraceEnabled() {
		return log.isTraceEnabled();
	}

	@Override
	public void trace(String msg) {
		log.trace(tag + msg);
	}

	@Override
	public void trace(String format, Object arg) {
		log.trace(tag + format, arg);
	}

	@Override
	public void trace(String format, Object arg1, Object arg2) {
		log.trace(tag + format, arg1, arg2);
	}

	@Override
	public void trace(String format, Object... arguments) {
		log.trace(tag + format, arguments);
	}

	@Override
	public void trace(String msg, Throwable t) {
		log.trace(tag + msg, t);
	}

	@Override
	public boolean isTraceEnabled(Marker marker) {
		return log.isTraceEnabled(marker);
	}

	@Override
	public void trace(Marker marker, String msg) {
		log.trace(marker, tag + msg);
	}

	@Override
	public void trace(Marker marker, String format, Object arg) {
		log.trace(marker, tag + format, arg);
	}

	@Override
	public void trace(Marker marker, String format, Object arg1, Object arg2) {
		log.trace(marker, tag + format, arg1, arg2);
	}

	@Override
	public void trace(Marker marker, String format, Object... argArray) {
		log.trace(marker, tag + format, argArray);
	}

	@Override
	public void trace(Marker marker, String msg, Throwable t) {
		log.trace(marker, tag + msg, t);
	}

	@Override
	public boolean isDebugEnabled() {
		return log.isDebugEnabled();
	}

	@Override
	public void debug(String msg) {
		log.debug(tag + msg);
		
	}

	@Override
	public void debug(String format, Object arg) {
		log.debug(tag + format, arg);
	}

	@Override
	public void debug(String format, Object arg1, Object arg2) {
		log.debug(tag + format, arg1, arg2);
	}

	@Override
	public void debug(String format, Object... arguments) {
		log.debug(tag + format, arguments);
	}

	@Override
	public void debug(String msg, Throwable t) {
		log.debug(tag + msg, t);
	}

	@Override
	public boolean isDebugEnabled(Marker marker) {
		return log.isDebugEnabled(marker);
	}

	@Override
	public void debug(Marker marker, String msg) {
		log.debug(marker, tag + msg);
	}

	@Override
	public void debug(Marker marker, String format, Object arg) {
		log.debug(marker, tag + format, arg);
	}

	@Override
	public void debug(Marker marker, String format, Object arg1, Object arg2) {
		log.debug(marker, tag + format, arg1, arg2);
	}

	@Override
	public void debug(Marker marker, String format, Object... arguments) {
		log.debug(marker, tag + format, arguments);
	}

	@Override
	public void debug(Marker marker, String msg, Throwable t) {
		log.debug(marker, tag + msg, t);
	}

	@Override
	public boolean isInfoEnabled() {
		return log.isInfoEnabled();
	}

	@Override
	public void info(String msg) {
		log.info(tag + msg);
	}

	@Override
	public void info(String format, Object arg) {
		log.info(tag + format, arg);
	}

	@Override
	public void info(String format, Object arg1, Object arg2) {
		log.info(tag + format, arg1, arg2);
	}

	@Override
	public void info(String format, Object... arguments) {
		log.info(tag + format, arguments);
	}

	@Override
	public void info(String msg, Throwable t) {
		log.info(tag + msg, t);
	}

	@Override
	public boolean isInfoEnabled(Marker marker) {
		return log.isInfoEnabled(marker);
	}

	@Override
	public void info(Marker marker, String msg) {
		log.info(marker, tag + msg);
	}

	@Override
	public void info(Marker marker, String format, Object arg) {
		log.info(marker, tag + format, arg);
	}

	@Override
	public void info(Marker marker, String format, Object arg1, Object arg2) {
		log.info(marker, tag + format, arg1, arg2);
	}

	@Override
	public void info(Marker marker, String format, Object... arguments) {
		log.info(marker, tag + format, arguments);
	}

	@Override
	public void info(Marker marker, String msg, Throwable t) {
		log.info(marker, tag + msg, t);
	}

	@Override
	public boolean isWarnEnabled() {
		return log.isWarnEnabled();
	}

	@Override
	public void warn(String msg) {
		log.warn(tag + msg);
	}

	@Override
	public void warn(String format, Object arg) {
		log.warn(tag + format, arg);
	}

	@Override
	public void warn(String format, Object... arguments) {
		log.warn(tag + format, arguments);
	}

	@Override
	public void warn(String format, Object arg1, Object arg2) {
		log.warn(tag + format, arg1, arg2);
	}

	@Override
	public void warn(String msg, Throwable t) {
		log.warn(tag + msg, t);
	}

	@Override
	public boolean isWarnEnabled(Marker marker) {
		return log.isWarnEnabled(marker);
	}

	@Override
	public void warn(Marker marker, String msg) {
		log.warn(marker, tag + msg);
	}

	@Override
	public void warn(Marker marker, String format, Object arg) {
		log.warn(marker, tag + format, arg);
	}

	@Override
	public void warn(Marker marker, String format, Object arg1, Object arg2) {
		log.warn(marker, tag + format, arg1, arg2);
	}

	@Override
	public void warn(Marker marker, String format, Object... arguments) {
		log.warn(marker, tag + format, arguments);
	}

	@Override
	public void warn(Marker marker, String msg, Throwable t) {
		log.warn(marker, tag + msg, t);
	}

	@Override
	public boolean isErrorEnabled() {
		return log.isErrorEnabled();
	}

	@Override
	public void error(String msg) {
		log.error(tag + msg);
	}

	@Override
	public void error(String format, Object arg) {
		log.error(tag + format, arg);
	}

	@Override
	public void error(String format, Object arg1, Object arg2) {
		log.error(tag + format, arg1, arg2);
	}

	@Override
	public void error(String format, Object... arguments) {
		log.error(tag + format, arguments);
	}

	@Override
	public void error(String msg, Throwable t) {
		log.error(tag + msg, t);
	}

	@Override
	public boolean isErrorEnabled(Marker marker) {
		return log.isErrorEnabled(marker);
	}

	@Override
	public void error(Marker marker, String msg) {
		log.error(marker, tag + msg);
	}

	@Override
	public void error(Marker marker, String format, Object arg) {
		log.error(marker, tag + format, arg);
	}

	@Override
	public void error(Marker marker, String format, Object arg1, Object arg2) {
		log.error(marker, tag + format, arg1, arg2);
	}

	@Override
	public void error(Marker marker, String format, Object... arguments) {
		log.error(marker, tag + format, arguments);
	}

	@Override
	public void error(Marker marker, String msg, Throwable t) {
		log.error(marker, tag + msg, t);
	}

}
