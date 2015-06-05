package org.orienteer.core.widget.support.jquery;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.resource.CssResourceReference;
import org.orienteer.core.widget.AbstractWidget;
import org.orienteer.core.widget.DashboardPanel;
import org.orienteer.core.widget.support.IDashboardSupport;
import org.orienteer.core.widget.support.gridster.GridsterDashboardSupport;

import com.orientechnologies.orient.core.record.impl.ODocument;

import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;

/**
 * {@link IDashboardSupport} for Jquery UI
 */
public class JQueryDashboardSupport implements IDashboardSupport {
	final static WebjarsJavaScriptResourceReference JQUERY_UI_JS = new WebjarsJavaScriptResourceReference("/jquery-ui/current/jquery-ui.min.js");
	final static CssResourceReference WIDGET_CSS = new CssResourceReference(JQueryDashboardSupport.class, "widget.css");
	
	@Override
	public void initDashboardPanel(DashboardPanel<?> dashboard) {
		dashboard.add(new JQueryDashboardAjaxBehavior());
	}

	@Override
	public void initWidget(AbstractWidget<?> widget) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ajaxAddWidget(AbstractWidget<?> widget, AjaxRequestTarget target) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ajaxDeleteWidget(AbstractWidget<?> widget,
			AjaxRequestTarget target) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveSettings(AbstractWidget<?> widget, ODocument doc) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void loadSettings(AbstractWidget<?> widget, ODocument doc) {
		// TODO Auto-generated method stub
		
	}
	

}
