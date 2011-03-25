/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Arne Kepp / The Open Planning Project 2009
 *  
 */
package org.geowebcache.storage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.geowebcache.grid.SRS;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.mime.ImageMime;
import org.geowebcache.mime.MimeType;
import org.geowebcache.storage.blobstore.file.FileBlobStore;

public class BlobStoreTest extends TestCase {
    public static final String TEST_BLOB_DIR_NAME = "gwcTestBlobs";

    public void testTile() throws Exception {
        FileBlobStore fbs = setup();

        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = { 1L, 2L, 3L };
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("a", "x");
        parameters.put("b", "ø");
        TileObject to = TileObject.createCompleteTileObject("test:123123 112", xyz, "EPSG:4326",
                "image/jpeg", parameters, bytes);
        to.setId(11231231);

        fbs.put(to);

        TileObject to2 = TileObject.createQueryTileObject("test:123123 112", xyz, "EPSG:4326",
                "image/jpeg", parameters);
        to2.setId(11231231);

        Resource resp = fbs.get(to2);

        to2.setBlob(resp);

        assertEquals(to.getBlobFormat(), to2.getBlobFormat());
        assertTrue(IOUtils.contentEquals(to.getBlob().getInputStream(), to2.getBlob()
                .getInputStream()));
    }

    public void testTileDelete() throws Exception {
        FileBlobStore fbs = setup();

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("a", "x");
        parameters.put("b", "ø");

        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = { 5L, 6L, 7L };
        TileObject to = TileObject.createCompleteTileObject("test:123123 112", xyz, "EPSG:4326",
                "image/jpeg", parameters, bytes);
        to.setId(11231231);

        fbs.put(to);

        TileObject to2 = TileObject.createQueryTileObject("test:123123 112", xyz, "EPSG:4326",
                "image/jpeg", parameters);
        to2.setId(11231231);

        Resource resp = fbs.get(to2);

        // to2.setBlob(resp);

        assertTrue(IOUtils.contentEquals(resp.getInputStream(), bytes.getInputStream()));

        TileObject to3 = TileObject.createQueryTileObject("test:123123 112", xyz, "EPSG:4326",
                "image/jpeg", parameters);
        fbs.delete(to3);

        TileObject to4 = TileObject.createQueryTileObject("test:123123 112", xyz, "EPSG:4326",
                "image/jpeg", parameters);
        assertNull(fbs.get(to4));
    }

    public void testTilRangeDelete() throws Exception {
        FileBlobStore fbs = setup();

        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("a", "x");
        parameters.put("b", "ø");
        MimeType mime = ImageMime.png;
        SRS srs = SRS.getEPSG4326();
        String layerName = "test:123123 112";

        int zoomLevel = 7;
        int x = 25;
        int y = 6;

        // long[] origXYZ = {x,y,zoomLevel};

        TileObject[] tos = new TileObject[6];

        for (int i = 0; i < tos.length; i++) {
            long[] xyz = { x + i - 1, y, zoomLevel };
            tos[i] = TileObject.createCompleteTileObject(layerName, xyz, srs.toString(),
                    mime.getFormat(), parameters, bytes);
            fbs.put(tos[i]);
        }

        long[][] rangeBounds = new long[zoomLevel + 2][4];
        int zoomStart = zoomLevel - 1;
        int zoomStop = zoomLevel + 1;

        long[] range = { x, y, x + tos.length - 3, y };
        rangeBounds[zoomLevel] = range;

        TileRange trObj = new TileRange(layerName, srs.toString(), zoomStart, zoomStop,
                rangeBounds, mime, parameters);

        fbs.delete(trObj);

        // starting x and x + tos.length should have data, the remaining should not
        TileObject firstTO = TileObject.createQueryTileObject(layerName, tos[0].xyz,
                srs.toString(), mime.getFormat(), parameters);
        assertTrue(IOUtils.contentEquals(fbs.get(firstTO).getInputStream(), bytes.getInputStream()));

        TileObject lastTO = TileObject.createQueryTileObject(layerName, tos[tos.length - 1].xyz,
                srs.toString(), mime.getFormat(), parameters);
        assertTrue(IOUtils.contentEquals(fbs.get(lastTO).getInputStream(), bytes.getInputStream()));

        TileObject midTO = TileObject.createQueryTileObject(layerName,
                tos[(tos.length - 1) / 2].xyz, srs.toString(), mime.getFormat(), parameters);
        Resource res = fbs.get(midTO);

        assertNull(res);
    }

    public FileBlobStore setup() throws Exception {
        File fh = new File(StorageBrokerTest.findTempDir() + File.separator + TEST_BLOB_DIR_NAME);

        if (!fh.exists() && !fh.mkdirs()) {
            throw new StorageException("Unable to create " + fh.getAbsolutePath());
        }

        return new FileBlobStore(StorageBrokerTest.findTempDir() + File.separator
                + TEST_BLOB_DIR_NAME);
    }
}
