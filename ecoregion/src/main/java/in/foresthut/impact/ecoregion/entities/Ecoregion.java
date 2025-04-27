package in.foresthut.impact.ecoregion.entities;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonMultiPolygon;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author maulik-dave
 */
@Document(collection = "ecoregion")
public class Ecoregion {
	@Id
	private String id;
	private int regionId;
	private String name;
	private List<String> countries;
	private String realm;
	private String biome;
	private String bioregion;
	private List<String> keyStoneSpecies;
	@JsonIgnore
	private GeoJsonMultiPolygon regionMap;
	private double areaHectares;

	public Ecoregion() {
		super();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getCountries() {
		return countries;
	}

	public void setCountries(List<String> countries) {
		this.countries = countries;
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getBiome() {
		return biome;
	}

	public void setBiome(String biome) {
		this.biome = biome;
	}

	public List<String> getKeyStoneSpecies() {
		return keyStoneSpecies;
	}

	public void setKeyStoneSpecies(List<String> keyStoneSpecies) {
		this.keyStoneSpecies = keyStoneSpecies;
	}

	public GeoJsonMultiPolygon getRegionMap() {
		return regionMap;
	}

	public void setRegionMap(GeoJsonMultiPolygon regionMap) {
		this.regionMap = regionMap;
	}

	public double getArea() {
		return areaHectares;
	}

	public void setArea(double area) {
		this.areaHectares = area;
	}

	public int getRegionId() {
		return regionId;
	}

	public void setRegionId(int regionId) {
		this.regionId = regionId;
	}

	public String getBioregion() {
		return bioregion;
	}

	public void setBioregion(String bioregion) {
		this.bioregion = bioregion;
	}

	@Override
	public String toString() {
		return "Ecoregion [id=" + id + ", regionId=" + regionId + ", name=" + name + ", countries=" + countries
				+ ", realm=" + realm + ", biome=" + biome + ", bioregion=" + bioregion + ", keyStoneSpecies="
				+ keyStoneSpecies + ", areaHectares=" + areaHectares + "]";
	}

}
