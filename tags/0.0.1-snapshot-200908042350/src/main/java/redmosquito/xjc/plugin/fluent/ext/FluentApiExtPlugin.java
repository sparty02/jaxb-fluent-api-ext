package redmosquito.xjc.plugin.fluent.ext;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JForLoop;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JPrimitiveType;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;

public class FluentApiExtPlugin extends Plugin {

	private String methodPrefix = "with";

	public String getOptionName() {
		return "Xfluent-api-ext";
	}

	public String getUsage() {
		return " Fluent API Extension";
	}

	public boolean run(Outline outline, Options options,
			ErrorHandler errorHandler) throws SAXException {
		for (ClassOutline classOutline : outline.getClasses()) {
			for (FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
				switch (getFieldType(fieldOutline)) {
				case ELEMENT:
					createElementMethod(fieldOutline);
					break;
				case COLLECTION:
					createCollectionMethod(fieldOutline);
					break;
				case OTHER:
				default:
					break;
				}
			}
		}
		return false;
	}

	protected void createElementMethod(FieldOutline fieldOutline) {
		final JDefinedClass implClass = fieldOutline.parent().implClass;
		final String fieldName = fieldOutline.getPropertyInfo().getName(false);
		final String propertyName = fieldOutline.getPropertyInfo()
				.getName(true);

		JMethod method = implClass.method(JMod.PUBLIC, fieldOutline
				.getRawType(), methodPrefix + propertyName);

		JBlock body = method.body();

		JConditional _if = body._if(JExpr.refthis(fieldName).eq(JExpr._null()));
		JBlock _then = _if._then();
		_then.assign(JExpr.ref(fieldName), JExpr
				._new(fieldOutline.getRawType()));

		body._return(JExpr.ref(fieldName));
	}

	protected void createCollectionMethod(FieldOutline fieldOutline) {
		final JDefinedClass implClass = fieldOutline.parent().implClass;
		final String propertyName = fieldOutline.getPropertyInfo()
				.getName(true);
		final JClass elementClass = fieldOutline.getRawType().boxify()
				.getTypeParameters().get(0);
		final JPrimitiveType INT = fieldOutline.parent().parent()
				.getCodeModel().INT;

		JMethod method = implClass.method(JMod.PUBLIC, elementClass,
				methodPrefix + propertyName);
		JVar index = method.param(INT, "index");

		JBlock body = method.body();

		JVar list = body.decl(fieldOutline.getRawType(), "list", JExpr._this()
				.invoke("get" + propertyName));
		JConditional _ifListIsTooSmall = body._if(list.invoke("size")
				.lte(index));
		JBlock _ifListIsTooSmallThen = _ifListIsTooSmall._then();
		JForLoop _for = _ifListIsTooSmallThen._for();
		JVar i = _for.init(INT, "i", list.invoke("size"));
		_for.test(i.lte(index));
		_for.update(i.incr());
		_for.body().invoke(list, "add").arg(JExpr._null());

		JVar element = body.decl(elementClass, "value", list.invoke("get").arg(
				index));
		JConditional _ifElementIsNull = body._if(element.eq(JExpr._null()));
		JBlock _ifElementIsNullThen = _ifElementIsNull._then();
		_ifElementIsNullThen.assign(element, JExpr._new(element.type()));
		_ifElementIsNullThen.invoke(list, "set").arg(index).arg(element);

		body._return(element);
	}

	protected FieldType getFieldType(FieldOutline fieldOutline) {
		return isElement(fieldOutline) ? FieldType.ELEMENT
				: isCollection(fieldOutline) ? FieldType.COLLECTION
						: FieldType.OTHER;
	}

	protected boolean isElement(FieldOutline fieldOutline) {
		JPackage classPackage = fieldOutline.parent()._package()._package();
		JPackage fieldPackage = fieldOutline.getRawType().boxify()._package();
		return classPackage.equals(fieldPackage);
	}

	protected boolean isCollection(FieldOutline fieldOutline) {
		return fieldOutline.getPropertyInfo().isCollection();
	}

	private static enum FieldType {
		ELEMENT, COLLECTION, OTHER
	}
}
