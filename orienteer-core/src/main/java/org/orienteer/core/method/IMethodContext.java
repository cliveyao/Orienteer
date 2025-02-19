package org.orienteer.core.method;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.model.IModel;
import org.orienteer.core.widget.AbstractWidget;

/**
 * 
 * Interface for setting method environment data.
 * We recommend using {@link MethodContext} instead your implementation of this interface.  
 *
 */
public interface IMethodContext {
	/**
	 * {@link IModel} for current displayed object
	 * @return {@link IModel}
	 */
	public IModel<?> getDisplayObjectModel();
	/**
	 * Current displayed widget
	 * @return current widget
	 */
	public AbstractWidget<?> getCurrentWidget();
	/**
	 * Current displayed widget type
	 * @return widget type
	 */
	public String getCurrentWidgetType();
	/**
	 * Current place
	 * @return {@link MethodPlace}
	 */
	public MethodPlace getPlace();
	/**
	 * Related UI component. Often with internal structure or additional data for {@link getDisplayObjectModel}
	 * Not mandatory
	 * @return table object
	 */
	public Component getRelatedComponent();
	
	public default void showFeedback(final int feedbackLevel, final String messageKey, final IModel<?> model) {
		AbstractWidget<?> widget = getCurrentWidget();
		if(widget!=null) {
			String message = widget.getLocalizer().getString(messageKey, widget, model);
			widget.getFeedbackMessages().add(new FeedbackMessage(widget, message, feedbackLevel));
			final Page page = widget.findParent(Page.class);
			if (page != null)page.dirty();
		}
	}
	
}
