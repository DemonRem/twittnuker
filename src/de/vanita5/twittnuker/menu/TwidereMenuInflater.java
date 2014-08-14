package de.vanita5.twittnuker.menu;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.ActionProvider;
import android.view.InflateException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import de.vanita5.twittnuker.util.ThemeUtils;

/**
 * This class is used to instantiate menu XML files into Menu objects.
 * <p/>
 * For performance reasons, menu inflation relies heavily on pre-processing of
 * XML files that is done at build time. Therefore, it is not currently possible
 * to use MenuInflater with an XmlPullParser over a plain XML file at runtime;
 * it only works with an XmlPullParser returned from a compiled resource (R.
 * <em>something</em> file.)
 */
public class TwidereMenuInflater {
	private static final String LOG_TAG = "MenuInflater";

	/**
	 * Menu tag name in XML.
	 */
	private static final String XML_MENU = "menu";

	/**
	 * Group tag name in XML.
	 */
	private static final String XML_GROUP = "group";

	/**
	 * Item tag name in XML.
	 */
	private static final String XML_ITEM = "item";

	private static final int NO_ID = 0;

	private static final Class<?>[] ACTION_VIEW_CONSTRUCTOR_SIGNATURE = new Class[]{Context.class};

	private static final Class<?>[] ACTION_PROVIDER_CONSTRUCTOR_SIGNATURE = ACTION_VIEW_CONSTRUCTOR_SIGNATURE;

	private final Object[] mActionViewConstructorArguments;

	private final Object[] mActionProviderConstructorArguments;

	private final Context mContext;
	private final Resources mResources;
	private final Object mRealOwner;

	/**
	 * Constructs a menu inflater.
	 *
	 * @see android.app.Activity#getMenuInflater()
	 */
	public TwidereMenuInflater(Context context) {
		this(context, context);
	}

	/**
	 * Constructs a menu inflater.
	 *
	 * @hide
	 * @see android.app.Activity#getMenuInflater()
	 */
	public TwidereMenuInflater(Context context, Object realOwner) {
		mContext = context;
		mResources = context.getResources();
		mRealOwner = realOwner;
		mActionViewConstructorArguments = new Object[]{context};
		mActionProviderConstructorArguments = mActionViewConstructorArguments;
	}

	/**
	 * Inflate a menu hierarchy from the specified XML resource. Throws
	 * {@link InflateException} if there is an error.
	 *
	 * @param menuRes Resource ID for an XML layout resource to load (e.g.,
	 *                <code>R.menu.main_activity</code>)
	 * @param menu    The Menu to inflate into. The items and submenus will be
	 *                added to this Menu.
	 */
	public void inflate(int menuRes, Menu menu) {
		XmlResourceParser parser = null;
		try {
			parser = mContext.getResources().getLayout(menuRes);
			AttributeSet attrs = Xml.asAttributeSet(parser);

			parseMenu(parser, attrs, menu);
		} catch (XmlPullParserException e) {
			throw new InflateException("Error inflating menu XML", e);
		} catch (IOException e) {
			throw new InflateException("Error inflating menu XML", e);
		} finally {
			if (parser != null) parser.close();
		}
	}

	/**
	 * Called internally to fill the given menu. If a sub menu is seen, it will
	 * call this recursively.
	 */
	private void parseMenu(XmlPullParser parser, AttributeSet attrs, Menu menu)
			throws XmlPullParserException, IOException {
		MenuState menuState = new MenuState(menu);

		int eventType = parser.getEventType();
		String tagName;
		boolean lookingForEndOfUnknownTag = false;
		String unknownTagName = null;

		// This loop will skip to the menu start tag
		do {
			if (eventType == XmlPullParser.START_TAG) {
				tagName = parser.getName();
				if (tagName.equals(XML_MENU)) {
					// Go to next tag
					eventType = parser.next();
					break;
				}

				throw new RuntimeException("Expecting menu, got " + tagName);
			}
			eventType = parser.next();
		} while (eventType != XmlPullParser.END_DOCUMENT);

		boolean reachedEndOfMenu = false;
		while (!reachedEndOfMenu) {
			switch (eventType) {
				case XmlPullParser.START_TAG:
					if (lookingForEndOfUnknownTag) {
						break;
					}

					tagName = parser.getName();
					if (tagName.equals(XML_GROUP)) {
						menuState.readGroup(attrs);
					} else if (tagName.equals(XML_ITEM)) {
						menuState.readItem(attrs);
					} else if (tagName.equals(XML_MENU)) {
						// A menu start tag denotes a submenu for an item
						SubMenu subMenu = menuState.addSubMenuItem();

						// Parse the submenu into returned SubMenu
						parseMenu(parser, attrs, subMenu);
					} else {
						lookingForEndOfUnknownTag = true;
						unknownTagName = tagName;
					}
					break;

				case XmlPullParser.END_TAG:
					tagName = parser.getName();
					if (lookingForEndOfUnknownTag && tagName.equals(unknownTagName)) {
						lookingForEndOfUnknownTag = false;
						unknownTagName = null;
					} else if (tagName.equals(XML_GROUP)) {
						menuState.resetGroup();
					} else if (tagName.equals(XML_ITEM)) {
						// Add the item if it hasn't been added (if the item was
						// a submenu, it would have been added already)
						if (!menuState.hasAddedItem()) {
							if (menuState.itemActionProvider != null &&
									menuState.itemActionProvider.hasSubMenu()) {
								menuState.addSubMenuItem();
							} else {
								menuState.addItem();
							}
						}
					} else if (tagName.equals(XML_MENU)) {
						reachedEndOfMenu = true;
					}
					break;

				case XmlPullParser.END_DOCUMENT:
					throw new RuntimeException("Unexpected end of document");
			}

			eventType = parser.next();
		}
	}

	private static class InflatedOnMenuItemClickListener
			implements MenuItem.OnMenuItemClickListener {
		private static final Class<?>[] PARAM_TYPES = new Class[]{MenuItem.class};

		private Object mRealOwner;
		private Method mMethod;

		public InflatedOnMenuItemClickListener(Object realOwner, String methodName) {
			mRealOwner = realOwner;
			Class<?> c = realOwner.getClass();
			try {
				mMethod = c.getMethod(methodName, PARAM_TYPES);
			} catch (Exception e) {
				InflateException ex = new InflateException(
						"Couldn't resolve menu item onClick handler " + methodName +
								" in class " + c.getName()
				);
				ex.initCause(e);
				throw ex;
			}
		}

		public boolean onMenuItemClick(MenuItem item) {
			try {
				if (mMethod.getReturnType() == Boolean.TYPE) {
					return (Boolean) mMethod.invoke(mRealOwner, item);
				} else {
					mMethod.invoke(mRealOwner, item);
					return true;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static class Styleable {

		public static final int[] MenuGroup = {android.R.attr.id, android.R.attr.menuCategory,
				android.R.attr.orderInCategory, android.R.attr.checkableBehavior,
				android.R.attr.visible, android.R.attr.enabled};
		public static final int MenuGroup_id = 0;
		public static final int MenuGroup_menuCategory = 1;
		public static final int MenuGroup_orderInCategory = 2;
		public static final int MenuGroup_checkableBehavior = 3;
		public static final int MenuGroup_visible = 4;
		public static final int MenuGroup_enabled = 5;

		public static final int[] MenuItem = {android.R.attr.id, android.R.attr.menuCategory,
				android.R.attr.orderInCategory, android.R.attr.title, android.R.attr.titleCondensed,
				android.R.attr.icon, android.R.attr.alphabeticShortcut, android.R.attr.numericShortcut,
				android.R.attr.checkable, android.R.attr.checked, android.R.attr.visible,
				android.R.attr.enabled, android.R.attr.showAsAction, android.R.attr.onClick,
				android.R.attr.actionLayout, android.R.attr.actionViewClass,
				android.R.attr.actionProviderClass};
		public static final int MenuItem_id = 0;
		public static final int MenuItem_menuCategory = 1;
		public static final int MenuItem_orderInCategory = 2;
		public static final int MenuItem_title = 3;
		public static final int MenuItem_titleCondensed = 4;
		public static final int MenuItem_icon = 5;
		public static final int MenuItem_alphabeticShortcut = 6;
		public static final int MenuItem_numericShortcut = 7;
		public static final int MenuItem_checkable = 8;
		public static final int MenuItem_checked = 9;
		public static final int MenuItem_visible = 10;
		public static final int MenuItem_enabled = 11;
		public static final int MenuItem_showAsAction = 12;
		public static final int MenuItem_onClick = 13;
		public static final int MenuItem_actionLayout = 14;
		public static final int MenuItem_actionViewClass = 15;
		public static final int MenuItem_actionProviderClass = 16;
	}

	/**
	 * State for the current menu.
	 * <p/>
	 * Groups can not be nested unless there is another menu (which will have
	 * its state class).
	 */
	private class MenuState {


		/**
		 * This is the part of an order integer that the user can provide.
		 *
		 * @hide
		 */
		static final int USER_MASK = 0x0000ffff;
		/**
		 * Bit shift of the user portion of the order integer.
		 *
		 * @hide
		 */
		static final int USER_SHIFT = 0;

		/**
		 * This is the part of an order integer that supplies the category of the
		 * item.
		 *
		 * @hide
		 */
		static final int CATEGORY_MASK = 0xffff0000;

		private Menu menu;

		/*
		 * Group state is set on items as they are added, allowing an item to
		 * override its group state. (As opposed to set on items at the group end tag.)
		 */
		private int groupId;
		private int groupCategory;
		private int groupOrder;
		private int groupCheckable;
		private boolean groupVisible;
		private boolean groupEnabled;

		private boolean itemAdded;
		private int itemId;
		private int itemCategoryOrder;
		private CharSequence itemTitle;
		private CharSequence itemTitleCondensed;
		private int itemIconResId;
		private char itemAlphabeticShortcut;
		private char itemNumericShortcut;
		/**
		 * Sync to attrs.xml enum:
		 * - 0: none
		 * - 1: all
		 * - 2: exclusive
		 */
		private int itemCheckable;
		private boolean itemChecked;
		private boolean itemVisible;
		private boolean itemEnabled;

		/**
		 * Sync to attrs.xml enum, values in MenuItem:
		 * - 0: never
		 * - 1: ifRoom
		 * - 2: always
		 * - -1: Safe sentinel for "no value".
		 */
		private int itemShowAsAction;

		private int itemActionViewLayout;
		private String itemActionViewClassName;
		private String itemActionProviderClassName;

		private String itemListenerMethodName;

		private ActionProvider itemActionProvider;

		private static final int defaultGroupId = NO_ID;
		private static final int defaultItemId = NO_ID;
		private static final int defaultItemCategory = 0;
		private static final int defaultItemOrder = 0;
		private static final int defaultItemCheckable = 0;
		private static final boolean defaultItemChecked = false;
		private static final boolean defaultItemVisible = true;
		private static final boolean defaultItemEnabled = true;

		public MenuState(final Menu menu) {
			this.menu = menu;

			resetGroup();
		}

		public void resetGroup() {
			groupId = defaultGroupId;
			groupCategory = defaultItemCategory;
			groupOrder = defaultItemOrder;
			groupCheckable = defaultItemCheckable;
			groupVisible = defaultItemVisible;
			groupEnabled = defaultItemEnabled;
		}

		/**
		 * Called when the parser is pointing to a group tag.
		 */
		public void readGroup(AttributeSet attrs) {
			TypedArray a = mContext.obtainStyledAttributes(attrs,
					Styleable.MenuGroup);

			groupId = a.getResourceId(Styleable.MenuGroup_id, defaultGroupId);
			groupCategory = a.getInt(Styleable.MenuGroup_menuCategory, defaultItemCategory);
			groupOrder = a.getInt(Styleable.MenuGroup_orderInCategory, defaultItemOrder);
			groupCheckable = a.getInt(Styleable.MenuGroup_checkableBehavior, defaultItemCheckable);
			groupVisible = a.getBoolean(Styleable.MenuGroup_visible, defaultItemVisible);
			groupEnabled = a.getBoolean(Styleable.MenuGroup_enabled, defaultItemEnabled);

			a.recycle();
		}

		/**
		 * Called when the parser is pointing to an item tag.
		 */
		public void readItem(AttributeSet attrs) {
			TypedArray a = mContext.obtainStyledAttributes(attrs,
					Styleable.MenuItem);

			// Inherit attributes from the group as default value
			itemId = a.getResourceId(Styleable.MenuItem_id, defaultItemId);
			final int category = a.getInt(Styleable.MenuItem_menuCategory, groupCategory);
			final int order = a.getInt(Styleable.MenuItem_orderInCategory, groupOrder);
			itemCategoryOrder = (category & CATEGORY_MASK) | (order & USER_MASK);
			itemTitle = a.getText(Styleable.MenuItem_title);
			itemTitleCondensed = a.getText(Styleable.MenuItem_titleCondensed);
			itemIconResId = ThemeUtils.findAttributeResourceValue(attrs, "icon", 0);
			itemAlphabeticShortcut =
					getShortcut(a.getString(Styleable.MenuItem_alphabeticShortcut));
			itemNumericShortcut =
					getShortcut(a.getString(Styleable.MenuItem_numericShortcut));
			if (a.hasValue(Styleable.MenuItem_checkable)) {
				// Item has attribute checkable, use it
				itemCheckable = a.getBoolean(Styleable.MenuItem_checkable, false) ? 1 : 0;
			} else {
				// Item does not have attribute, use the group's (group can have one more state
				// for checkable that represents the exclusive checkable)
				itemCheckable = groupCheckable;
			}
			itemChecked = a.getBoolean(Styleable.MenuItem_checked, defaultItemChecked);
			itemVisible = a.getBoolean(Styleable.MenuItem_visible, groupVisible);
			itemEnabled = a.getBoolean(Styleable.MenuItem_enabled, groupEnabled);
			itemShowAsAction = a.getInt(Styleable.MenuItem_showAsAction, -1);
			itemListenerMethodName = a.getString(Styleable.MenuItem_onClick);
			itemActionViewLayout = a.getResourceId(Styleable.MenuItem_actionLayout, 0);
			itemActionViewClassName = a.getString(Styleable.MenuItem_actionViewClass);
			itemActionProviderClassName = a.getString(Styleable.MenuItem_actionProviderClass);

			final boolean hasActionProvider = itemActionProviderClassName != null;
			if (hasActionProvider && itemActionViewLayout == 0 && itemActionViewClassName == null) {
				itemActionProvider = newInstance(itemActionProviderClassName,
						ACTION_PROVIDER_CONSTRUCTOR_SIGNATURE,
						mActionProviderConstructorArguments);
			} else {
				if (hasActionProvider) {
					Log.w(LOG_TAG, "Ignoring attribute 'actionProviderClass'."
							+ " Action view already specified.");
				}
				itemActionProvider = null;
			}

			a.recycle();

			itemAdded = false;
		}

		private char getShortcut(String shortcutString) {
			if (shortcutString == null) {
				return 0;
			} else {
				return shortcutString.charAt(0);
			}
		}

		private void setItem(MenuItem item) {
			item.setChecked(itemChecked)
					.setVisible(itemVisible)
					.setEnabled(itemEnabled)
					.setCheckable(itemCheckable >= 1)
					.setTitleCondensed(itemTitleCondensed)
					.setIcon(itemIconResId > 0 ? mResources.getDrawable(itemIconResId) : null)
					.setAlphabeticShortcut(itemAlphabeticShortcut)
					.setNumericShortcut(itemNumericShortcut);

			if (itemShowAsAction >= 0) {
				item.setShowAsAction(itemShowAsAction);
			}

			if (itemListenerMethodName != null) {
				if (mContext.isRestricted()) {
					throw new IllegalStateException("The android:onClick attribute cannot "
							+ "be used within a restricted context");
				}
				item.setOnMenuItemClickListener(
						new InflatedOnMenuItemClickListener(mRealOwner, itemListenerMethodName));
			}

//            if (item instanceof MenuItemImpl) {
//                MenuItemImpl impl = (MenuItemImpl) item;
//                if (itemCheckable >= 2) {
//                    impl.setExclusiveCheckable(true);
//                }
//            }

			boolean actionViewSpecified = false;
			if (itemActionViewClassName != null) {
				View actionView = (View) newInstance(itemActionViewClassName,
						ACTION_VIEW_CONSTRUCTOR_SIGNATURE, mActionViewConstructorArguments);
				item.setActionView(actionView);
				actionViewSpecified = true;
			}
			if (itemActionViewLayout > 0) {
				if (!actionViewSpecified) {
					item.setActionView(itemActionViewLayout);
					actionViewSpecified = true;
				} else {
					Log.w(LOG_TAG, "Ignoring attribute 'itemActionViewLayout'."
							+ " Action view already specified.");
				}
			}
			if (itemActionProvider != null) {
				item.setActionProvider(itemActionProvider);
			}
		}

		public void addItem() {
			itemAdded = true;
			setItem(menu.add(groupId, itemId, itemCategoryOrder, itemTitle));
		}

		public SubMenu addSubMenuItem() {
			itemAdded = true;
			SubMenu subMenu = menu.addSubMenu(groupId, itemId, itemCategoryOrder, itemTitle);
			setItem(subMenu.getItem());
			return subMenu;
		}

		public boolean hasAddedItem() {
			return itemAdded;
		}

		@SuppressWarnings("unchecked")
		private <T> T newInstance(String className, Class<?>[] constructorSignature,
								  Object[] arguments) {
			try {
				Class<?> clazz = mContext.getClassLoader().loadClass(className);
				Constructor<?> constructor = clazz.getConstructor(constructorSignature);
				return (T) constructor.newInstance(arguments);
			} catch (Exception e) {
				Log.w(LOG_TAG, "Cannot instantiate class: " + className, e);
			}
			return null;
		}
	}
}