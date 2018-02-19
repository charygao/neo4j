/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.internal.cypher.acceptance

import java.util.concurrent.TimeUnit

import org.neo4j.cypher.ExecutionEngineFunSuite
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.spatial.Point
import org.neo4j.internal.cypher.acceptance.CypherComparisonSupport._
import org.neo4j.values.storable.{CoordinateReferenceSystem, Values}

class SpatialFunctionsAcceptanceTest extends ExecutionEngineFunSuite with CypherComparisonSupport {

  val pointConfig = Configs.Interpreted - Configs.Version2_3
  val distanceAndEqualityConfig = Configs.Interpreted - Configs.OldAndRule
  val indexConfig = Configs.Interpreted - Configs.BackwardsCompatibility - Configs.AllRulePlanners

  test("point function should work with literal map") {
    val result = executeWith(pointConfig, "RETURN point({latitude: 12.78, longitude: 56.7}) as point",
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "point"),
        expectPlansToFail = Configs.AllRulePlanners))

    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 56.7, 12.78))))
  }

  test("point function should work with literal map and cartesian coordinates") {
    val result = executeWith(pointConfig, "RETURN point({x: 2.3, y: 4.5, crs: 'cartesian'}) as point",
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "point"),
        expectPlansToFail = Configs.AllRulePlanners))

    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.Cartesian, 2.3, 4.5))))
  }

  test("point function should work with literal map and geographic coordinates") {
    val result = executeWith(pointConfig, "RETURN point({longitude: 2.3, latitude: 4.5, crs: 'WGS-84'}) as point",
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "point"),
      expectPlansToFail = Configs.AllRulePlanners))

    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 2.3, 4.5))))
  }

  test("point function should not work with literal map and incorrect cartesian CRS") {
    failWithError(pointConfig, "RETURN point({x: 2.3, y: 4.5, crs: 'cart'}) as point", List("'cart' is not a supported coordinate reference system for points",
      "Unknown coordinate reference system: cart"))
  }

  test("point function should not work with literal map and incorrect geographic CRS") {
    failWithError(pointConfig, "RETURN point({x: 2.3, y: 4.5, crs: 'WGS84'}) as point", List("'WGS84' is not a supported coordinate reference system for points",
      "Unknown coordinate reference system: WGS84"))
  }

  test("point function should work with integer arguments") {
    val result = executeWith(pointConfig, "RETURN point({x: 2, y: 4}) as point",
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "point"),
        expectPlansToFail = Configs.AllRulePlanners))

    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.Cartesian, 2, 4))))
  }

  test("should fail properly if missing cartesian coordinates") {
    failWithError(pointConfig, "RETURN point({params}) as point", List("A point must contain either 'x' and 'y' or 'latitude' and 'longitude'"),
      params = "params" -> Map("y" -> 1.0, "crs" -> "cartesian"))
  }

  test("should fail properly if missing geographic longitude") {
    failWithError(pointConfig, "RETURN point({params}) as point", List("A point must contain either 'x' and 'y' or 'latitude' and 'longitude'"),
      params = "params" -> Map("latitude" -> 1.0, "crs" -> "WGS-84"))
  }

  test("should fail properly if missing geographic latitude") {
    failWithError(pointConfig, "RETURN point({params}) as point", List("A point must contain either 'x' and 'y' or 'latitude' and 'longitude'"),
      params = "params" -> Map("longitude" -> 1.0, "crs" -> "WGS-84"))
  }

  test("should fail properly if unknown coordinate system") {
    failWithError(pointConfig, "RETURN point({params}) as point", List("'WGS-1337' is not a supported coordinate reference system for points",
      "Unknown coordinate reference system: WGS-1337"),
      params = "params" -> Map("x" -> 1, "y" -> 2, "crs" -> "WGS-1337"))
  }

  test("should default to Cartesian if missing cartesian CRS") {
    val result = executeWith(pointConfig, "RETURN point({x: 2.3, y: 4.5}) as point",
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "point"),
        expectPlansToFail = Configs.AllRulePlanners))

    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.Cartesian, 2.3, 4.5))))
  }

  test("should default to WGS84 if missing geographic CRS") {
    val result = executeWith(pointConfig, "RETURN point({longitude: 2.3, latitude: 4.5}) as point",
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "point"),
        expectPlansToFail = Configs.AllRulePlanners))

    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 2.3, 4.5))))
  }

  test("should allow Geographic CRS with x/y coordinates") {
    val result = executeWith(pointConfig, "RETURN point({x: 2.3, y: 4.5, crs: 'WGS-84'}) as point",
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "point"),
        expectPlansToFail = Configs.AllRulePlanners))

    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 2.3, 4.5))))
  }

  test("should not allow Cartesian CRS with latitude/longitude coordinates") {
    failWithError(pointConfig, "RETURN point({longitude: 2.3, latitude: 4.5, crs: 'cartesian'}) as point",
      List("'cartesian' is not a supported coordinate reference system for geographic points",
        "Geographic points does not support coordinate reference system: cartesian"))
  }

  test("point function should work with previous map") {
    val result = executeWith(pointConfig, "WITH {latitude: 12.78, longitude: 56.7} as data RETURN point(data) as point",
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "point"),
        expectPlansToFail = Configs.AllRulePlanners))

    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 56.7, 12.78))))
  }

  test("distance function should work on co-located points") {
    val result = executeWith(pointConfig, "WITH point({latitude: 12.78, longitude: 56.7}) as point RETURN distance(point,point) as dist",
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "point", "dist"),
        expectPlansToFail = Configs.AllRulePlanners))

    result.toList should equal(List(Map("dist" -> 0.0)))
  }

  test("distance function should work on nearby cartesian points") {
    val result = executeWith(pointConfig,
      """
        |WITH point({x: 2.3, y: 4.5, crs: 'cartesian'}) as p1, point({x: 1.1, y: 5.4, crs: 'cartesian'}) as p2
        |RETURN distance(p1,p2) as dist
      """.stripMargin,
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "p1", "p2", "dist"),
        expectPlansToFail = Configs.AllRulePlanners))

    result.columnAs("dist").next().asInstanceOf[Double] should equal(1.5)
  }

  test("distance function should work on nearby points") {
    val result = executeWith(pointConfig,
      """
        |WITH point({longitude: 12.78, latitude: 56.7}) as p1, point({latitude: 56.71, longitude: 12.79}) as p2
        |RETURN distance(p1,p2) as dist
      """.stripMargin,
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "p1", "p2", "dist"),
        expectPlansToFail = Configs.AllRulePlanners))

    Math.round(result.columnAs("dist").next().asInstanceOf[Double]) should equal(1270)
  }

  test("distance function should work on distant points") {
    val result = executeWith(pointConfig,
      """
        |WITH point({latitude: 56.7, longitude: 12.78}) as p1, point({longitude: -51.9, latitude: -16.7}) as p2
        |RETURN distance(p1,p2) as dist
      """.stripMargin,
    planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "p1", "p2", "dist"),
      expectPlansToFail = Configs.AllRulePlanners))

    Math.round(result.columnAs("dist").next().asInstanceOf[Double]) should equal(10116214)
  }

  test("distance function should not fail if provided with points from different CRS") {
    val localConfig = pointConfig - Configs.OldAndRule
      val res = executeWith(localConfig,
        """WITH point({x: 2.3, y: 4.5, crs: 'cartesian'}) as p1, point({longitude: 1.1, latitude: 5.4, crs: 'WGS-84'}) as p2
        |RETURN distance(p1,p2) as dist""".stripMargin)
    res.columnAs[AnyRef]("dist").next() should be (null)
  }

  test("distance function should measure distance from Copenhagen train station to Neo4j in Malmö") {
    val result = executeWith(pointConfig,
      """
        |WITH point({latitude: 55.672874, longitude: 12.564590}) as p1, point({latitude: 55.611784, longitude: 12.994341}) as p2
        |RETURN distance(p1,p2) as dist
      """.stripMargin,
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "p1", "p2","dist"),
        expectPlansToFail = Configs.AllRulePlanners))

    Math.round(result.columnAs("dist").next().asInstanceOf[Double]) should equal(27842)
  }

  test("distance function should work with two null inputs") {
    val result = executeWith(pointConfig, "RETURN distance(null, null) as dist")
    result.toList should equal(List(Map("dist" -> null)))
  }

  test("distance function should return null with lhs null input") {
    val result = executeWith(pointConfig,
      """
        |WITH point({latitude: 55.672874, longitude: 12.564590}) as p1
        |RETURN distance(null, p1) as dist
      """.stripMargin)
    result.toList should equal(List(Map("dist" -> null)))
  }

  test("distance function should return null with rhs null input") {
    val result = executeWith(pointConfig,
      """
        |WITH point({latitude: 55.672874, longitude: 12.564590}) as p1
        |RETURN distance(p1, null) as dist
      """.stripMargin)
    result.toList should equal(List(Map("dist" -> null)))
  }

  test("distance function should return null if a point is null") {
    var result = executeWith(pointConfig,
      "RETURN distance(point({latitude:3,longitude:7}),point({latitude:null, longitude:3})) as dist;")
    result.toList should equal(List(Map("dist" -> null)))

    result = executeWith(pointConfig,
      "RETURN distance(point({latitude:3,longitude:null}),point({latitude:7, longitude:3})) as dist;")
    result.toList should equal(List(Map("dist" -> null)))

    result = executeWith(pointConfig,
      "RETURN distance(point({x:3,y:7}),point({x:null, y:3})) as dist;")
    result.toList should equal(List(Map("dist" -> null)))

    result = executeWith(pointConfig,
      "RETURN distance(point({x:3,y:null}),point({x:7, y:3})) as dist;")
    result.toList should equal(List(Map("dist" -> null)))
  }

  test("distance function should fail on wrong type") {
    val config = Configs.AbsolutelyAll + TestConfiguration(Versions.Default, Planners.Default, Runtimes.Default) - Configs.Version2_3
    failWithError(config, "RETURN distance(1, 2) as dist", List("Type mismatch: expected Point or Geometry but was Integer"))
  }

  test("point function should work with node properties") {
    // Given
    createLabeledNode(Map("latitude" -> 12.78, "longitude" -> 56.7), "Place")

    // When
    val result = executeWith(pointConfig - Configs.Morsel, "MATCH (p:Place) RETURN point({latitude: p.latitude, longitude: p.longitude}) as point",
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "point"),
        expectPlansToFail = Configs.AllRulePlanners))

    // Then
    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 56.7, 12.78))))
  }

  test("point function should work with relationship properties") {
    // Given
    val r = relate(createNode(), createNode(), "PASS_THROUGH", Map("latitude" -> 12.78, "longitude" -> 56.7))

    // When
    val result = executeWith(pointConfig, "MATCH ()-[r:PASS_THROUGH]->() RETURN point({latitude: r.latitude, longitude: r.longitude}) as point",
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "point"),
        expectPlansToFail = Configs.AllRulePlanners))

    // Then
    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 56.7, 12.78))))
  }

  test("point function should work with node as map") {
    // Given
    createLabeledNode(Map("latitude" -> 12.78, "longitude" -> 56.7), "Place")

    // When
    val result = executeWith(pointConfig - Configs.Morsel, "MATCH (p:Place) RETURN point(p) as point",
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "point"),
        expectPlansToFail = Configs.AllRulePlanners))

    // Then
    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 56.7, 12.78))))
  }

  test("point function should work with null input") {
    val result = executeWith(pointConfig, "RETURN point(null) as p")
    result.toList should equal(List(Map("p" -> null)))
  }

  test("point function should return null if the map that backs it up contains a null") {
    var result = executeWith(pointConfig, "RETURN point({latitude:null, longitude:3}) as pt;")
    result.toList should equal(List(Map("pt" -> null)))

    result = executeWith(pointConfig, "RETURN point({latitude:3, longitude:null}) as pt;")
    result.toList should equal(List(Map("pt" -> null)))

    result = executeWith(pointConfig, "RETURN point({x:null, y:3}) as pt;")
    result.toList should equal(List(Map("pt" -> null)))

    result = executeWith(pointConfig, "RETURN point({x:3, y:null}) as pt;")
    result.toList should equal(List(Map("pt" -> null)))
  }

  test("point function should fail on wrong type") {
    val config = Configs.AbsolutelyAll + TestConfiguration(Versions.Default, Planners.Default, Runtimes.Default) - Configs.Version2_3
    failWithError(config, "RETURN point(1) as dist", List("Type mismatch: expected Map, Node or Relationship but was Integer"))
  }

  test("point should be assignable to node property") {
    // Given
    createLabeledNode("Place")

    // When
    val result = executeWith(distanceAndEqualityConfig, "MATCH (p:Place) SET p.location = point({latitude: 56.7, longitude: 12.78}) RETURN p.location as point",
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "point"),
        expectPlansToFail = Configs.AllRulePlanners))

    // Then
    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 12.78, 56.7))))
  }

  test("point should be readable from node property") {
    // Given
    createLabeledNode("Place")
    graph.execute("MATCH (p:Place) SET p.location = point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'}) RETURN p.location as point")

    // When
    val result = executeWith(Configs.All, "MATCH (p:Place) RETURN p.location as point",
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "point"),
        expectPlansToFail = Configs.AllRulePlanners))

    // Then
    val point = result.columnAs("point").toList.head.asInstanceOf[Point]
    point should equal(Values.pointValue(CoordinateReferenceSystem.WGS84, 12.78, 56.7))
    // And CRS names should equal
    point.getCRS.getHref should equal("http://spatialreference.org/ref/epsg/4326/")
  }

  test("indexed point should be readable from node property") {
    // Given
    graph.inTx {
      graph.schema().indexFor(Label.label("Place")).on("location").create()
    }
    graph.inTx {
      graph.schema().awaitIndexesOnline(5, TimeUnit.SECONDS)
    }
    createLabeledNode("Place")
    graph.execute("MATCH (p:Place) SET p.location = point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'}) RETURN p.location as point")

    // When
    val localConfig = pointConfig - Configs.AllRulePlanners
    val result = executeWith(localConfig,
      "MATCH (p:Place) WHERE p.location = point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'}) RETURN p.location as point",
      planComparisonStrategy = ComparePlansWithAssertion({ plan =>
        plan should useOperatorWithText("Projection", "point")
        plan should useOperatorWithText("NodeIndexSeek", ":Place(location)")
      }, expectPlansToFail = Configs.AbsolutelyAll - Configs.Version3_4 - Configs.Version3_3))

    // Then
    val point = result.columnAs("point").toList.head.asInstanceOf[Point]
    point should equal(Values.pointValue(CoordinateReferenceSystem.WGS84, 12.78, 56.7))
    // And CRS names should equal
    point.getCRS.getHref should equal("http://spatialreference.org/ref/epsg/4326/")
  }

  test("with multiple indexed points only exact match should be returned") {
    // Given
    graph.inTx {
      graph.schema().indexFor(Label.label("Place")).on("location").create()
    }
    graph.inTx {
      graph.schema().awaitIndexesOnline(5, TimeUnit.SECONDS)
    }
    createLabeledNode("Place")
    graph.execute("MATCH (p:Place) SET p.location = point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'}) RETURN p.location as point")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 40.7, longitude: -35.78, crs: 'WGS-84'})")

    val configuration = TestConfiguration(Versions(Versions.V3_3, Versions.V3_4, Versions.Default), Planners(Planners.Cost, Planners.Default), Runtimes(Runtimes.Interpreted, Runtimes.Slotted, Runtimes.Default))
    // When
    val result = executeWith(configuration,
      "MATCH (p:Place) WHERE p.location = point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'}) RETURN p.location as point",
      planComparisonStrategy = ComparePlansWithAssertion({ plan =>
        plan should useOperatorWithText("Projection", "point")
        plan should useOperatorWithText("NodeIndexSeek", ":Place(location)")
      }, expectPlansToFail = Configs.AbsolutelyAll - configuration))

    // Then
    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 12.78, 56.7))))
  }

  test("indexed points far apart in cartesian space - range query greaterThan") {
    // Given
    graph.createIndex("Place", "location")
    graph.execute("CREATE (p:Place) SET p.location = point({x: 100000, y: 100000})")
    graph.execute("CREATE (p:Place) SET p.location = point({x: -100000, y: 100000})")
    graph.execute("CREATE (p:Place) SET p.location = point({x: -100000, y: -100000})")
    graph.execute("CREATE (p:Place) SET p.location = point({x: 100000, y: -100000})")

    // When
    val result = executeWith(indexConfig, "CYPHER MATCH (p:Place) WHERE p.location > point({x: 0, y: 0}) RETURN p.location as point")

    // Then
    val plan = result.executionPlanDescription()
    plan should useOperatorWithText("Projection", "point")
    plan should useOperatorWithText("NodeIndexSeekByRange", ":Place(location) > point")
    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.Cartesian, 100000, 100000))))
  }

  test("indexed points far apart in cartesian space - range query lessThan") {
    // Given
    graph.createIndex("Place", "location")
    graph.execute("CREATE (p:Place) SET p.location = point({x: 100000, y: 100000})")
    graph.execute("CREATE (p:Place) SET p.location = point({x: -100000, y: 100000})")
    graph.execute("CREATE (p:Place) SET p.location = point({x: -100000, y: -100000})")
    graph.execute("CREATE (p:Place) SET p.location = point({x: 100000, y: -100000})")

    // When
    val result = executeWith(indexConfig, "CYPHER MATCH (p:Place) WHERE p.location < point({x: 0, y: 0}) RETURN p.location as point")

    // Then
    val plan = result.executionPlanDescription()
    plan should useOperatorWithText("Projection", "point")
    plan should useOperatorWithText("NodeIndexSeekByRange", ":Place(location) < point")
    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.Cartesian, -100000, -100000))))
  }

  test("indexed points far apart in cartesian space - range query within") {
    // Given
    graph.createIndex("Place", "location")
    graph.execute("CREATE (p:Place) SET p.location = point({x: 500000, y: 500000})")
    graph.execute("CREATE (p:Place) SET p.location = point({x: 100000, y: 100000})")
    graph.execute("CREATE (p:Place) SET p.location = point({x: -100000, y: 100000})")
    graph.execute("CREATE (p:Place) SET p.location = point({x: -100000, y: -100000})")
    graph.execute("CREATE (p:Place) SET p.location = point({x: 100000, y: -100000})")

    // When
    val result = executeWith(indexConfig, "CYPHER MATCH (p:Place) WHERE p.location > point({x: 0, y: 0}) AND p.location < point({x: 200000, y: 200000}) RETURN p.location as point")

    // Then
    val plan = result.executionPlanDescription()
    plan should useOperatorWithText("Projection", "point")
    plan should useOperatorWithText("NodeIndexSeekByRange", ":Place(location) > point", ":Place(location) < point")
    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.Cartesian, 100000, 100000))))
  }

  test("indexed points far apart in WGS84 - range query greaterThan") {
    // Given
    graph.createIndex("Place", "location")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 5.7, longitude: 116.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: -50.7, longitude: 12.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.7, longitude: -10.78, crs: 'WGS-84'})")

    // When
    val result = executeWith(indexConfig, "CYPHER MATCH (p:Place) WHERE p.location > point({latitude: 56.0, longitude: 12.0, crs: 'WGS-84'}) RETURN p.location as point")

    // Then
    val plan = result.executionPlanDescription()
    plan should useOperatorWithText("Projection", "point")
    plan should useOperatorWithText("NodeIndexSeekByRange", ":Place(location) > point")
    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 12.78, 56.7))))
  }

  test("indexed points close together in WGS84 - equality query") {
    // Given
    graph.createIndex("Place", "location")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.700001, longitude: 12.7800001, crs: 'WGS-84'})")

    // When
    val result = executeWith(distanceAndEqualityConfig, "CYPHER MATCH (p:Place) WHERE p.location = point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'}) RETURN p.location as point")

    // Then
    val plan = result.executionPlanDescription()
    plan should useOperatorWithText("NodeIndexSeek", ":Place(location)")
    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 12.78, 56.7))))
  }

  test("Index query with MERGE") {
    // Given
    graph.createIndex("Place", "location")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.700001, longitude: 12.7800001, crs: 'WGS-84'})")

    // When matching in merge
    val result = executeWith(distanceAndEqualityConfig, "MERGE (p:Place {location: point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'}) }) RETURN p.location as point")

    // Then
    val plan = result.executionPlanDescription()
    plan should useOperatorWithText("NodeIndexSeek", ":Place(location)")
    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 12.78, 56.7))))

    //  And when creating in merge
    val result2 = executeWith(distanceAndEqualityConfig, "MERGE (p:Place {location: point({latitude: 156.7, longitude: 112.78, crs: 'WGS-84'}) }) RETURN p.location as point")

    // Then
    val plan2 = result2.executionPlanDescription()
    plan2 should useOperatorWithText("NodeIndexSeek", ":Place(location)")
    result2.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 112.78, 156.7))))
  }

  test("indexed points close together in WGS84 - range query greaterThan") {
    // Given
    graph.createIndex("Place", "location")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 55.7, longitude: 11.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 50.7, longitude: 12.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.7, longitude: 10.78, crs: 'WGS-84'})")

    // When
    val result = executeWith(indexConfig, "CYPHER MATCH (p:Place) WHERE p.location > point({latitude: 56.0, longitude: 12.0, crs: 'WGS-84'}) RETURN p.location as point")

    // Then
    val plan = result.executionPlanDescription()
    plan should useOperatorWithText("Projection", "point")
    plan should useOperatorWithText("NodeIndexSeekByRange", ":Place(location) > point")
    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 12.78, 56.7))))
  }

  test("indexed points close together in WGS84 - range query greaterThanOrEqualTo") {
    // Given
    graph.createIndex("Place", "location")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 55.7, longitude: 11.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 50.7, longitude: 12.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.7, longitude: 10.78, crs: 'WGS-84'})")

    // When
    val result = executeWith(indexConfig, "CYPHER MATCH (p:Place) WHERE p.location >= point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'}) RETURN p.location as point")

    // Then
    val plan = result.executionPlanDescription()
    plan should useOperatorWithText("Projection", "point")
    plan should useOperatorWithText("NodeIndexSeekByRange", ":Place(location) >= point")
    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 12.78, 56.7))))
  }

  test("indexed points close together in WGS84 - range query greaterThan with no results") {
    // Given
    graph.createIndex("Place", "location")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 55.7, longitude: 11.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 50.7, longitude: 12.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.7, longitude: 10.78, crs: 'WGS-84'})")

    // When
    val result = executeWith(indexConfig, "CYPHER MATCH (p:Place) WHERE p.location > point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'}) RETURN p.location as point")

    // Then
    val plan = result.executionPlanDescription()
    plan should useOperatorWithText("Projection", "point")
    plan should useOperatorWithText("NodeIndexSeekByRange", ":Place(location) > point")
    assert(result.isEmpty)
  }

  test("indexed points close together in WGS84 - range query greaterThan with multiple CRS") {
    // Given
    graph.createIndex("Place", "location")
    graph.execute("CREATE (p:Place) SET p.location = point({y: 56.7, x: 12.78, crs: 'cartesian'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 55.7, longitude: 11.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 50.7, longitude: 12.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.7, longitude: 10.78, crs: 'WGS-84'})")

    // When
    val result = executeWith(indexConfig, "CYPHER MATCH (p:Place) WHERE p.location >= point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'}) RETURN p.location as point")

    // Then
    val plan = result.executionPlanDescription()
    plan should useOperatorWithText("Projection", "point")
    plan should useOperatorWithText("NodeIndexSeekByRange", ":Place(location) >= point")
    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 12.78, 56.7))))
  }

  test("indexed points close together in WGS84 - range query within") {
    // Given
    graph.createIndex("Place", "location")
    graph.execute("CREATE (p:Place) SET p.location = point({y: 55.7, x: 11.78, crs: 'cartesian'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 55.7, longitude: 11.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 50.7, longitude: 12.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.7, longitude: 10.78, crs: 'WGS-84'})")

    // When
    val result = executeWith(indexConfig, "CYPHER MATCH (p:Place) WHERE p.location >= point({latitude: 55.7, longitude: 11.78, crs: 'WGS-84'}) AND p.location < point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'}) RETURN p.location as point")

    // Then
    val plan = result.executionPlanDescription()
    plan should useOperatorWithText("Projection", "point")
    plan should useOperatorWithText("NodeIndexSeekByRange", ":Place(location) >= point", ":Place(location) < point")
    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 11.78, 55.7))))
  }

  test("points with distance query and mixed crs") {
    // Given
    graph.execute("CREATE (p:Place) SET p.location = point({y: 55.7, x: 11.78, crs: 'cartesian'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.7, longitude: 12.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 55.7, longitude: 11.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 50.7, longitude: 12.78, crs: 'WGS-84'})")
    graph.execute("CREATE (p:Place) SET p.location = point({latitude: 56.7, longitude: 10.78, crs: 'WGS-84'})")

    val query =
      """CYPHER MATCH (p:Place)
        |WHERE distance(p.location, point({latitude: 55.7, longitude: 11.78, crs: 'WGS-84'})) < 1000
        |RETURN p.location as point
      """.stripMargin
    // When
    val result = executeWith(distanceAndEqualityConfig, query)

    // Then
    val plan = result.executionPlanDescription()
    plan should useOperatorWithText("Projection", "point")
    plan should useOperatorWithText("Filter", "distance")
    result.toList should equal(List(Map("point" -> Values.pointValue(CoordinateReferenceSystem.WGS84, 11.78, 55.7))))
  }

  test("indexed points with distance query and points within bbox") {
    // Given
    graph.createIndex("Place", "location")
    graph.execute("CREATE (p:Place) SET p.location = point({y: -10, x: -10, crs: 'cartesian'})")
    graph.execute("CREATE (p:Place) SET p.location = point({y: -10, x: 10, crs: 'cartesian'})")
    graph.execute("CREATE (p:Place) SET p.location = point({y: 10, x: -10, crs: 'cartesian'})")
    graph.execute("CREATE (p:Place) SET p.location = point({y: 10, x: 10, crs: 'cartesian'})")
    graph.execute("CREATE (p:Place) SET p.location = point({y: -10, x: 0, crs: 'cartesian'})")
    graph.execute("CREATE (p:Place) SET p.location = point({y: 10, x: 0, crs: 'cartesian'})")
    graph.execute("CREATE (p:Place) SET p.location = point({y: 0, x: -10, crs: 'cartesian'})")
    graph.execute("CREATE (p:Place) SET p.location = point({y: 0, x: 10, crs: 'cartesian'})")
    graph.execute("CREATE (p:Place) SET p.location = point({y: 0, x: 0, crs: 'cartesian'})")
    graph.execute("CREATE (p:Place) SET p.location = point({y: 9.99, x: 0, crs: 'cartesian'})")
    Range(11, 100).foreach(i => graph.execute(s"CREATE (p:Place) SET p.location = point({y: $i, x: $i, crs: 'cartesian'})"))

    // <=
    {
      val query =
        s"""CYPHER MATCH (p:Place)
           |WHERE distance(p.location, point({x: 0, y: 0, crs: 'cartesian'})) <= 10
           |RETURN p.location as point
        """.stripMargin
      // When
      val result = executeWith(distanceAndEqualityConfig, query)

      // Then
      val plan = result.executionPlanDescription()
      plan should useOperatorWithText("Projection", "point")
      plan should useOperatorWithText("Filter", "distance")
      plan should useOperatorWithText("NodeIndexSeekByRange", ":Place(location)", "distance", "<= ")
      result.toList.toSet should equal(Set(
        Map("point" -> Values.pointValue(CoordinateReferenceSystem.Cartesian, 10, 0)),
        Map("point" -> Values.pointValue(CoordinateReferenceSystem.Cartesian, 0, 10)),
        Map("point" -> Values.pointValue(CoordinateReferenceSystem.Cartesian, -10, 0)),
        Map("point" -> Values.pointValue(CoordinateReferenceSystem.Cartesian, 0, -10)),
        Map("point" -> Values.pointValue(CoordinateReferenceSystem.Cartesian, 0, 0)),
        Map("point" -> Values.pointValue(CoordinateReferenceSystem.Cartesian, 0, 9.99))
      ))
    }
    // <
    {
      val query =
        s"""CYPHER MATCH (p:Place)
           |WHERE distance(p.location, point({x: 0, y: 0, crs: 'cartesian'})) < 10
           |RETURN p.location as point
        """.stripMargin
      // When
      val result = executeWith(distanceAndEqualityConfig, query)

      // Then
      val plan = result.executionPlanDescription()
      plan should useOperatorWithText("Projection", "point")
      plan should useOperatorWithText("Filter", "distance")
      plan should useOperatorWithText("NodeIndexSeekByRange", ":Place(location)", "distance", "< ")
      result.toList.toSet should equal(Set(
        Map("point" -> Values.pointValue(CoordinateReferenceSystem.Cartesian, 0, 0)),
        Map("point" -> Values.pointValue(CoordinateReferenceSystem.Cartesian, 0, 9.99))
      ))
    }
  }

  test("invalid location with index") {
    // Given
    graph.createIndex("Place", "location")
    graph.execute("CREATE (p:Place) SET p.location = 5")
    Range(11, 100).foreach(i => graph.execute(s"CREATE (p:Place) SET p.location = point({y: $i, x: $i, crs: 'cartesian'})"))

    val query =
      s"""CYPHER MATCH (p:Place)
         |WHERE distance(p.location, point({x: 0, y: 0, crs: 'cartesian'})) <= 10
         |RETURN p.location as point
        """.stripMargin
    // When
    val result = executeWith(distanceAndEqualityConfig, query)

    // Then
    val plan = result.executionPlanDescription()
    plan should useOperatorWithText("Projection", "point")
    plan should useOperatorWithText("Filter", "distance")
    plan should useOperatorWithText("NodeIndexSeekByRange", ":Place(location)", "distance", "<= ")
    result.toList.toSet should equal(Set.empty)
  }

  test("invalid location without index") {
    // Given
    graph.execute("CREATE (p:Place) SET p.location = 5")
    Range(11, 100).foreach(i => graph.execute(s"CREATE (p:Place) SET p.location = point({y: $i, x: $i, crs: 'cartesian'})"))

    val query =
      s"""CYPHER MATCH (p:Place)
         |WHERE distance(p.location, point({x: 0, y: 0, crs: 'cartesian'})) <= 10
         |RETURN p.location as point
        """.stripMargin
    // When
    val result = executeWith(distanceAndEqualityConfig, query)

    // Then
    val plan = result.executionPlanDescription()
    plan should useOperatorWithText("Projection", "point")
    plan should useOperatorWithText("Filter", "distance")
    plan should useOperatorWithText("NodeByLabelScan", ":Place")
    result.toList.toSet should equal(Set.empty)
  }

  test("no error for distance with no point") {
    // Given
    graph.createIndex("Place", "location")
    graph.execute("CREATE (p:Place) SET p.location = point({y: 0, x: 0, crs: 'cartesian'})")
    Range(11, 100).foreach(i => graph.execute(s"CREATE (p:Place) SET p.location = point({y: $i, x: $i, crs: 'cartesian'})"))

    val query =
      """CYPHER MATCH (p:Place)
         |WHERE distance(p.location, $poi) <= 10
         |RETURN p.location as point
        """.stripMargin
    // When
    val result = executeWith(distanceAndEqualityConfig, query, params = Map("poi" -> 5))

    // Then
    result.toList shouldBe empty

    // And given
    graph.execute(s"DROP INDEX ON :Place(location)")
    // when
    val resultNoIndex = executeWith(distanceAndEqualityConfig, query,  params = Map("poi" -> 5))

    // Then
    resultNoIndex.toList shouldBe empty
  }

  test("array of points should be assignable to node property") {
    // Given
    createLabeledNode("Place")

    // When
    val query =
      """
        |UNWIND [1,2,3] as num
        |WITH point({x: num, y: num}) as p
        |WITH collect(p) as points
        |MATCH (place:Place) SET place.location = points
        |RETURN points
      """.stripMargin
    val result = executeWith(distanceAndEqualityConfig, query,
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "point"),
        expectPlansToFail = Configs.AllRulePlanners))

    // Then
    result.toList should equal(List(Map("points" -> List(
      Values.pointValue(CoordinateReferenceSystem.Cartesian, 1.0, 1.0),
      Values.pointValue(CoordinateReferenceSystem.Cartesian, 2.0, 2.0),
      Values.pointValue(CoordinateReferenceSystem.Cartesian, 3.0, 3.0)
    ))))
  }

  test("array of cartesian points should be readable from node property") {
    // Given
    createLabeledNode("Place")
    graph.execute(
      """
        |UNWIND [1,2,3] as num
        |WITH point({x: num, y: num}) as p
        |WITH collect(p) as points
        |MATCH (place:Place) SET place.location = points
        |RETURN place.location as points
      """.stripMargin)

    // When
    val result = executeWith(Configs.All, "MATCH (p:Place) RETURN p.location as points",
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "point"),
        expectPlansToFail = Configs.AllRulePlanners))

    // Then
    val points = result.columnAs("points").toList.head.asInstanceOf[Array[_]]
    points should equal(Array(
      Values.pointValue(CoordinateReferenceSystem.Cartesian, 1.0, 1.0),
      Values.pointValue(CoordinateReferenceSystem.Cartesian, 2.0, 2.0),
      Values.pointValue(CoordinateReferenceSystem.Cartesian, 3.0, 3.0)
    ))
  }

  test("array of wgs84 points should be readable from node property") {
    // Given
    createLabeledNode("Place")
    graph.execute(
      """
        |UNWIND [1,2,3] as num
        |WITH point({latitude: num, longitude: num}) as p
        |WITH collect(p) as points
        |MATCH (place:Place) SET place.location = points
        |RETURN place.location as points
      """.stripMargin)

    // When
    val result = executeWith(Configs.All, "MATCH (p:Place) RETURN p.location as points",
      planComparisonStrategy = ComparePlansWithAssertion(_ should useOperatorWithText("Projection", "point"),
        expectPlansToFail = Configs.AllRulePlanners))

    // Then
    val points = result.columnAs("points").toList.head.asInstanceOf[Array[_]]
    points should equal(Array(
      Values.pointValue(CoordinateReferenceSystem.WGS84, 1.0, 1.0),
      Values.pointValue(CoordinateReferenceSystem.WGS84, 2.0, 2.0),
      Values.pointValue(CoordinateReferenceSystem.WGS84, 3.0, 3.0)
    ))
  }

  test("array of mixed points should not be assignable to node property") {
    // Given
    createLabeledNode("Place")

    // When
    val query =
      """
        |WITH [point({x: 1, y: 2}), point({latitude: 1, longitude: 2})] as points
        |MATCH (place:Place) SET place.location = points
        |RETURN points
      """.stripMargin

    // Then
    failWithError(distanceAndEqualityConfig + Configs.Procs, query, Seq("Collections containing point values with different CRS can not be stored in properties."))
  }
}
