/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.cfg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.unicore.samly2.SAMLConstants;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.ExtensionsType;
import xmlbeans.org.oasis.saml2.metadata.LocalizedNameType;
import xmlbeans.org.oasis.saml2.metadata.OrganizationType;
import xmlbeans.org.oasis.saml2.metadata.SSODescriptorType;
import xmlbeans.org.oasis.saml2.metadata.extui.LogoType;
import xmlbeans.org.oasis.saml2.metadata.extui.UIInfoDocument;
import xmlbeans.org.oasis.saml2.metadata.extui.UIInfoType;

/**
 * A collection of methods helpful in reading SAML metadata
 * 
 * @author P.Piernik
 *
 */
public class MetaToConfigConverterHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, MetaToConfigConverterHelper.class);

	
	public static boolean supportsSaml2(SSODescriptorType idpDef)
	{
		List<?> supportedProtocols = idpDef.getProtocolSupportEnumeration();
		for (Object supported: supportedProtocols)
			if (SAMLConstants.PROTOCOL_NS.equals(supported))
				return true;
		return false;
	}
	
	public static UIInfoType parseMDUIInfo(ExtensionsType extensions, String entityId)
	{
		if (extensions == null)
			return null;
		NodeList nl = extensions.getDomNode().getChildNodes();
		for (int i = 0; i < nl.getLength(); i++)
		{
			Node elementN = nl.item(i);
			if (elementN.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element element = (Element) elementN;
			if ("UIInfo".equals(element.getLocalName())
					&& "urn:oasis:names:tc:SAML:metadata:ui".equals(element.getNamespaceURI()))
			{
				try
				{
					return UIInfoDocument.Factory.parse(element).getUIInfo();
				} catch (XmlException e)
				{
					log.warn("Can not parse UIInfo metadata extension for " + entityId, e);
				}
			}
		}
		return null;
	}

	public static Map<String, String> getLocalizedNames(MessageSource msg, UIInfoType uiInfo,
			SSODescriptorType idpDesc, EntityDescriptorType mainDescriptor)
	{
		Map<String, String> ret = new HashMap<String, String>();
		OrganizationType mainOrg = mainDescriptor.getOrganization();
		if (mainOrg != null)
		{
			addLocalizedNames(msg, mainOrg.getOrganizationNameArray(), ret);
			addLocalizedNames(msg, mainOrg.getOrganizationDisplayNameArray(), ret);
		}
		OrganizationType org = idpDesc.getOrganization();
		if (org != null)
		{
			addLocalizedNames(msg, org.getOrganizationNameArray(), ret);
			addLocalizedNames(msg, org.getOrganizationDisplayNameArray(), ret);
		}
		if (uiInfo != null)
		{
			addLocalizedNames(msg, uiInfo.getDisplayNameArray(), ret);
		}
		return ret;
	}

	public static Map<String, LogoType> getLocalizedLogos(UIInfoType uiInfo)
	{
		Map<String, LogoType> ret = new HashMap<String, LogoType>();
		if (uiInfo != null)
		{
			LogoType[] logos = uiInfo.getLogoArray();
			if (logos == null)
				return ret;
			for (LogoType logo : logos)
			{
				String key = logo.getLang() == null ? "" : "." + logo.getLang();
				LogoType e = ret.get(key);
				if (e == null)
				{
					ret.put(key, logo);
				} else
				{
					if (e.getHeight().longValue() < logo.getHeight().longValue())
						ret.put(key, logo);
				}
			}
		}
		return ret;
	}

	/**
	 * Converts SAML names to a language key->value map. All language keys
	 * are prefixed with dot. The empty key is used to provide a default
	 * value, for the default system locale. If such default was not found
	 * in the SAML names, then the 'en' locale is tried. Not fully correct,
	 * but this is de facto standard default locale for international
	 * federations.
	 * 
	 * @param names
	 * @param ret
	 */
	public static void addLocalizedNames(MessageSource msg, LocalizedNameType[] names, Map<String, String> ret)
	{
		if (names == null)
			return;
		String enName = null;
		for (LocalizedNameType name : names)
		{
			String lang = name.getLang();
			if (lang != null)
			{
				ret.put("." + lang, name.getStringValue());
				if (lang.equals(msg.getDefaultLocaleCode()))
					ret.put("", name.getStringValue());
				if (lang.equals("en"))
					enName = name.getStringValue();
			} else
			{
				ret.put("", name.getStringValue());
			}
		}
		if (enName != null && !ret.containsKey(""))
			ret.put("", enName);
	}
}
