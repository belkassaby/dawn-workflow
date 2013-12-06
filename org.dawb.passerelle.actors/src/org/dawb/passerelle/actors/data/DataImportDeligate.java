package org.dawb.passerelle.actors.data;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dawb.common.python.PythonUtils;
import org.dawb.common.util.io.FileUtils;
import org.dawb.passerelle.common.message.AbstractDatasetProvider;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.passerelle.common.parameter.ParameterUtils;
import org.dawnsci.io.h5.H5LazyDataset;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.BooleanToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.dataset.ILazyDataset;
import uk.ac.diamond.scisoft.analysis.io.DataHolder;
import uk.ac.diamond.scisoft.analysis.io.IDataHolder;
import uk.ac.diamond.scisoft.analysis.io.IMetaData;
import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;

import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.util.ptolemy.StringChoiceParameter;
import com.isencia.passerelle.util.ptolemy.StringMapParameter;
import com.isencia.passerelle.workbench.util.MapUtils;

/**
 * Used to delegate
 * @author fcp94556
 *
 */
class DataImportDelegate {
	
	private static final Logger logger = LoggerFactory.getLogger(DataImportDelegate.class);
	
	private final StringParameter         path;
	private final StringChoiceParameter   names;
	private final Parameter               relativePathParam;
	private final StringMapParameter      rename;
	
	DataImportDelegate(final StringParameter path, StringChoiceParameter   names, final Parameter relativePathParam, StringMapParameter rename) {
		this.path              = path;
		this.names             = names;
		this.relativePathParam = relativePathParam;
		this.rename            = rename;
	}
	
	private boolean isPathRelative() {
		try {
			return ((BooleanToken)relativePathParam.getToken()).booleanValue();
		} catch (Exception ne) {
			logger.error("Cannot find out if relative path");
			return false;
		}
	}
	
	public String getSourcePath() {
		return getSourcePath((ManagedMessage)null);
	}

	public String getSourcePath(final ManagedMessage manMsg) {

		String sourcePath=null;
		try {
			final DataMessageComponent comp = manMsg!=null ? MessageUtils.coerceMessage(manMsg) : null;
			sourcePath = ParameterUtils.getSubstituedValue(this.path, comp);
		} catch (Exception e) {
			// Can happen when they have an expand in the parameter that
			// is not resolved until run time.
			logger.info("Cannot substitute parameter "+path, e);
		}

		if (isPathRelative()) {
			try {
				final IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(sourcePath, true);
				if (res==null) return  null;
				sourcePath = res.getLocation().toOSString();
			} catch (NullPointerException ne) {
				return null;
			}
		}
		
        //getStandardMessageHeaders().put(ManagedMessage.SystemHeader.HEADER_SOURCE_INFO,sourcePath);

		return sourcePath;
	}
	
	public String getSourcePath(final List<DataMessageComponent> cache) {

		String sourcePath=null;
		try {
			final DataMessageComponent comp = cache!=null ? MessageUtils.mergeAll(cache) : null;
			sourcePath = ParameterUtils.getSubstituedValue(this.path, comp);
		} catch (Exception e) {
			// Can happen when they have an expand in the parameter that
			// is not resolved until run time.
			logger.info("Cannot substitute parameter "+path, e);
		}

		if (isPathRelative()) {
			try {
				final IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(sourcePath, true);
				if (res==null) return  null;
				sourcePath = res.getLocation().toOSString();
			} catch (NullPointerException ne) {
				return null;
			}
		}
		
        //getStandardMessageHeaders().put(ManagedMessage.SystemHeader.HEADER_SOURCE_INFO,sourcePath);

		return sourcePath;
	}


	private Collection<String>  cachedDatasets = null;	
	private Map<String,int[]>   cachedShapes   = null;	
	
    protected Collection<String> getAllDatasetsInFile() {
		if (cachedDatasets==null && getSourcePath()!=null) {
			try {
				String path = getSourcePath();
				File   file = new File(path);
				if (!file.exists()) return null;
				
				// For directories we assume that all files contain the same data sets.
				if (file.isDirectory()) {
					final File[] files = file.listFiles();
					for (int i = 0; i < files.length; i++) {
						if (!files[i].isDirectory()) {
							file = files[i];
							break;
						}
					}
				}
				
				final IMetaData meta  = LoaderFactory.getMetaData(file.getAbsolutePath(), null);
				if (meta!=null && meta.getDataNames()!=null) {
				    Collection<String> names = meta.getDataNames();
				    Map<String,int[]>  shapes= meta.getDataShapes();
				    
				    // If single image, rename
				    if (names!=null&&names.size()==1&&shapes!=null&&shapes.size()==1 && shapes.get(names.iterator().next())!=null && shapes.get(names.iterator().next()).length==2) {
				    	final int[] shape = shapes.get(names.iterator().next());
				    	cachedDatasets = Arrays.asList(new String[]{"image"});
				    	cachedShapes   = new HashMap<String,int[]>(1);
				    	cachedShapes.put("image", shape);
				    	
				    } else {
				    	cachedDatasets = names;
				    	cachedShapes   = shapes;
				    }
				}
			} catch (Exception e) {
				logger.error("Cannot get data set names from "+getSourcePath(), e);
				cachedDatasets = Collections.emptyList();
				cachedShapes   = Collections.emptyMap();
			}
		}
		if (cachedDatasets!=null&&!cachedDatasets.isEmpty()) {
			return cachedDatasets;
		}
		return null;
	}
    
	public Map<String,String> getChoppedNames() {
		getAllDatasetsInFile();
		return getChoppedNames(cachedDatasets);
	}


	public Map<String, String> getDataSetNameMap() {
		final String map = this.rename.getExpression();
		if (map == null) return null;
		final Map<String,String> nameMap = MapUtils.getMap(map);
		if (nameMap==null || nameMap.isEmpty()) return null;
		return nameMap;
	}
	
	public String getMappedName(final String hdfName) {
		final Map<String,String> nameMap = getDataSetNameMap();
        if (nameMap==null) return hdfName;
        if (!nameMap.containsKey(hdfName)) return hdfName;
        return nameMap.get(hdfName);
	}

	/**
	 * 
	 * @param names
	 * @return
	 */
	private static Map<String,String> getChoppedNames(final Collection<String> names) {
		
		final String rootName = getRootName(names);
		if (rootName==null)      return null;
		if (rootName.length()<1) return null;

		final Map<String,String> chopped = new HashMap<String,String>(names.size());
		for (String name : names) {
			chopped.put(name, name.substring(rootName.length()));
		}
		return chopped;
	}
	private static final Pattern ROOT_PATTERN = Pattern.compile("(\\/[a-zA-Z0-9]+\\/).+");

	public static String getRootName(Collection<String> names) {
		
		if (names==null) return null;
		String rootName = null;
		for (String name : names) {
			final Matcher matcher = ROOT_PATTERN.matcher(name);
			if (matcher.matches()) {
				final String rName = matcher.group(1);
				if (rootName!=null && !rootName.equals(rName)) {
					rootName = null;
					break;
				}
				rootName = rName;
			} else {
				rootName = null;
				break;
			}
		}
		return rootName;
	}

	public void clear() {
		cachedDatasets = null;
		cachedShapes   = null;
	}

	public Map<String, int[]> getShapes() {
		return cachedShapes;
	}
	
	public Map<String, String> getDataSetsRenameName() {
		
		final Collection<String>  sets = getAllDatasetsInFile();
        if (sets==null || sets.isEmpty()) return null;
		
		String rootName = DataImportDelegate.getRootName(sets);
        if (rootName==null) rootName = "";
        
		final Map<String,String> ret  = new LinkedHashMap<String,String>(7);
		for (String setName : sets) {
			try {
				ret.put(setName, PythonUtils.getLegalVarName(setName.substring(rootName.length()), ret.values()));
			} catch (Exception e) {
				ret.put(setName, setName);
			}
		}
		
		if (rename.getExpression()==null) {
			return ret;
		}
		
		final Map<String,String> existing = MapUtils.getMap(rename.getExpression());
		if (existing!=null) {
			existing.keySet().retainAll(ret.keySet());
			ret.putAll(existing);
		}
		
		return ret;
	}


	private static boolean isSingleImage(Map<String, ILazyDataset> data) {
		if (data.size()!=1) return false;
		final ILazyDataset set = data.values().iterator().next();
		return set.getShape()!=null && set.getShape().length==2;
	}


	private AbstractDataset getLoadedData(ILazyDataset lazy) throws Exception {
		
		if (lazy instanceof H5LazyDataset) {
		    return ((H5LazyDataset)lazy).getCompleteData(null);
		} else if (lazy instanceof AbstractDataset) {
			return (AbstractDataset) lazy;
		}

		return DatasetUtils.convertToAbstractDataset(lazy.getSlice());
	}

	
	
	public Map<String,Serializable> getDatasets(String filePath, final TriggerObject trigOb) throws Exception {
				
		final String[] ds     = names.getValue();

		// TODO Eclipse progress?
		Map<String, ILazyDataset> data = null;
		if (ds!=null&&ds.length>0) {
			final IDataHolder dh = LoaderFactory.getData(filePath);
			if (dh!=null) {
				data = dh.toLazyMap();
				data.keySet().retainAll(Arrays.asList(ds));
			}
		}
		
		if (data == null) {
			DataHolder dh = LoaderFactory.getData(filePath, null);
			data = dh.getMap();
			if (ds!=null&&ds.length>0) {
				data.keySet().retainAll(Arrays.asList(ds));
			}
		}
		
		final Map<String, Object> raw;
		if (ds==null) {
			raw = new LinkedHashMap<String, Object>();
			if (DataImportDelegate.isSingleImage(data)) {
				final ILazyDataset image = data.values().iterator().next();
				final String       name  = trigOb!=null && trigOb.getIndex()>-1
						                 ? "image"
						                 : "image"+trigOb.getIndex();
				image.setName(name);
				raw.put(name, image);
			} else {
			    raw.putAll(data);
			}
		} else {
			raw = new LinkedHashMap<String, Object>();
			for (String name : ds) {
				raw.put(name, data.get(name));
			}
		}
		
		
		final Map<String,String> nameMap = getDataSetNameMap();
		final Map<String,Serializable> ret = new HashMap<String,Serializable>(raw.size());
		
		// Set name and not to print data in string
		for (String name : raw.keySet()) {

			final ILazyDataset lazy = (ILazyDataset)raw.get(name);
			if (lazy==null) continue;
			
			if (nameMap!=null) {
				final String newName = nameMap.get(name);
				if (newName!=null && !"".equals(newName)) {
					name = newName;
				}
			}

			/**
			 * We load the data, this is an import actor
			 */
			final AbstractDataset set = getLoadedData(lazy);
			set.setName(name);
			
			ret.put(name, set);

		}
		
	    return ret;
	}
	
	
	public List<IVariable> getOutputVariables(boolean isFullData) {
		
		final List<IVariable> ret = new ArrayList<IVariable>(7);
		if (getSourcePath()==null)  {
			final String msg = "Invalid Path '"+path.getExpression()+"'";
			ret.add(new Variable("file_path", VARIABLE_TYPE.PATH,   msg, String.class));
			ret.add(new Variable("file_name", VARIABLE_TYPE.SCALAR, msg, String.class));
			ret.add(new Variable("file_dir",  VARIABLE_TYPE.PATH,   msg, String.class));
			return ret;
		}
		
		ret.add(new Variable("file_path", VARIABLE_TYPE.PATH, getSourcePath(), String.class));
		ret.add(new Variable("file_name", VARIABLE_TYPE.SCALAR, new File(getSourcePath()).getName(), String.class));
		ret.add(new Variable("file_dir",  VARIABLE_TYPE.PATH, FileUtils.getDirectory(getSourcePath()), String.class));
		
		if (isFullData) {
			
			Collection<String> all = getAllDatasetsInFile();// Sets up cache of sizes, which means we can return VARIABLE_TYPE.IMAGE
			
			String[] ds     = names.getValue();
			if (ds==null||ds.length<1) ds = all.toArray(new String[all.size()]);
			if (ds!=null&&ds.length>0) {
				
				Map<String, int[]> cachedShapes = getShapes();// Sets up cache of sizes, which means we can return VARIABLE_TYPE.IMAGE
				for (int i = 0; i < ds.length; i++) {
					final Map<String,String> varNames = getDataSetNameMap();
					final String name  = varNames!=null&&varNames.containsKey(ds[i])
					                   ? varNames.get(ds[i])
					                   : ds[i];
					if (cachedShapes!=null) {
						final int[] shape = cachedShapes.get(ds[i]);
						final VARIABLE_TYPE type = shape!=null&&shape.length==2
						                         ? VARIABLE_TYPE.IMAGE
						                         : VARIABLE_TYPE.ARRAY;
						final AbstractDatasetProvider example = new AbstractDatasetProvider(shape);
					    ret.add(new Variable(name, type, example, AbstractDataset.class));
	
					} else {
					    ret.add(new Variable(name, VARIABLE_TYPE.ARRAY, new AbstractDatasetProvider(), AbstractDataset.class));
					}
				}
			}
		}
		
		return ret;
	}


}