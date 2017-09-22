# -*- encoding: utf-8 -*-

import pytest

from sorter_logic import Decision, sort_image


def collection_image_data(**kwargs):
    image_data = {
        "image_title": "Image Title",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Paperclips and hairnets",
        "image_tech_captured_mode": "Frog retina",
        "image_copyright_cleared": "Y",
        "image_access_restrictions": "CC-BY",
        "image_general_use": "Y",
        "image_innopac_id": "12345678"
    }
    collection = 'images-M'
    if 'collection' in kwargs.keys():
        collection = kwargs.pop('collection')
    image_data.update(kwargs)

    return collection, image_data


def image_with_no_info(collection, image_title):
    return collection_image_data(collection=collection,
                                 image_title=image_title,
                                 image_pub_title=None,
                                 image_pub_periodical=None,
                                 image_innopac_id="1234567")


@pytest.mark.parametrize('collection, image_data', [
    collection_image_data(collection='images-F'),
    collection_image_data(collection='images-L', image_library_dept="Archives and Manuscripts"),
    collection_image_data(collection='images-V', image_library_dept="Archives and Manuscripts"),
    collection_image_data(collection='images-M', image_library_dept="Archives and Manuscripts"),
    collection_image_data(collection='images-L', image_tech_captured_mode="videodisc"),
    collection_image_data(collection='images-V', image_tech_captured_mode="videodisc"),
    collection_image_data(collection='images-M', image_tech_captured_mode="videodisc"),
    image_with_no_info(collection='images-L', image_title=None),
    image_with_no_info(collection='images-V', image_title=None),
    image_with_no_info(collection='images-M', image_title=None),
    image_with_no_info(collection='images-L', image_title="-"),
    image_with_no_info(collection='images-V', image_title="-"),
    image_with_no_info(collection='images-M', image_title="-"),
    image_with_no_info(collection='images-L', image_title="--"),
    image_with_no_info(collection='images-V', image_title="--"),
    image_with_no_info(collection='images-M', image_title="--"),
    image_with_no_info(collection='images-L', image_title="Awaiting description"),
    image_with_no_info(collection='images-V', image_title="Awaiting description"),
    image_with_no_info(collection='images-M', image_title="Awaiting description"),
    image_with_no_info(collection='images-L', image_title="Awaiting removal from MIRO as duplicate"),
    image_with_no_info(collection='images-V', image_title="Awaiting removal from MIRO as duplicate"),
    image_with_no_info(collection='images-M', image_title="Awaiting removal from MIRO as duplicate"),
    image_with_no_info(collection='images-L', image_title="Awaiting captions and consent form"),
    image_with_no_info(collection='images-V', image_title="Awaiting captions and consent form"),
    image_with_no_info(collection='images-M', image_title="Awaiting captions and consent form"),
    image_with_no_info(collection='images-L', image_title="Awaiting catalogue details"),
    image_with_no_info(collection='images-V', image_title="Awaiting catalogue details"),
    image_with_no_info(collection='images-M', image_title="Awaiting catalogue details"),
    image_with_no_info(collection='images-L', image_title="Awaiting caption info"),
    image_with_no_info(collection='images-V', image_title="Awaiting caption info"),
    image_with_no_info(collection='images-M', image_title="Awaiting caption info"),
    image_with_no_info(collection='images-L', image_title="No info available about this object"),
    image_with_no_info(collection='images-V', image_title="No info available about this object"),
    image_with_no_info(collection='images-M', image_title="No info available about this object"),
    image_with_no_info(collection='images-L', image_title="No neg"),
    image_with_no_info(collection='images-V', image_title="No neg"),
    image_with_no_info(collection='images-M', image_title="No neg"),
])
def test_is_cold_store(collection, image_data):
    """These examples all end up in cold store."""
    assert sort_image(collection, image_data) == Decision.cold_store


@pytest.mark.parametrize('collection, image_data', [
    collection_image_data(collection='images-L', image_library_dept="Public programmes"),
    collection_image_data(collection='images-V', image_library_dept="Public programmes"),
    collection_image_data(collection='images-M', image_library_dept="Public programmes"),
    collection_image_data(collection='images-L', image_tech_scanned_date="02/03/2016"),
    collection_image_data(collection='images-L', image_tech_scanned_date="30/06/2018"),
    collection_image_data(collection='images-L', image_copyright_cleared="N"),
    collection_image_data(collection='images-V', image_copyright_cleared="N"),
    collection_image_data(collection='images-M', image_copyright_cleared="N"),
    collection_image_data(collection='images-L', image_general_use="N"),
    collection_image_data(collection='images-V', image_general_use="N"),
    collection_image_data(collection='images-M', image_general_use="N"),
    collection_image_data(collection='images-L', image_access_restrictions="Top-secret"),
    collection_image_data(collection='images-V', image_access_restrictions="Top-secret"),
    collection_image_data(collection='images-M', image_access_restrictions="Top-secret")
])
def test_is_tandem_vault(collection, image_data):
    """These examples all end up in Tandem Vault."""
    assert sort_image(collection, image_data) == Decision.tandem_vault


@pytest.mark.parametrize('collection, image_data', [
    collection_image_data(),
    collection_image_data(image_innopac_id="1234567x"),
    collection_image_data(image_title=None,
                          image_pub_title=None,
                          image_pub_periodical=None,
                          image_innopac_id="1234567x"),
    collection_image_data(collection='images-L', image_tech_scanned_date="01/03/2016"),
    collection_image_data(collection='images-L', image_tech_scanned_date="29/02/2016"),
    collection_image_data(collection='images-L', image_access_restrictions=None),
    collection_image_data(collection='images-V', image_access_restrictions=None),
    collection_image_data(collection='images-M', image_access_restrictions=None),
    collection_image_data(collection='images-L', image_access_restrictions="CC-0"),
    collection_image_data(collection='images-V', image_access_restrictions="CC-0"),
    collection_image_data(collection='images-M', image_access_restrictions="CC-0"),
    collection_image_data(collection='images-L', image_access_restrictions="CC-BY"),
    collection_image_data(collection='images-V', image_access_restrictions="CC-BY"),
    collection_image_data(collection='images-M', image_access_restrictions="CC-BY"),
    collection_image_data(collection='images-L', image_access_restrictions="CC-BY-NC"),
    collection_image_data(collection='images-V', image_access_restrictions="CC-BY-NC"),
    collection_image_data(collection='images-M', image_access_restrictions="CC-BY-NC"),
    collection_image_data(collection='images-L', image_access_restrictions="CC-BY-NC-ND"),
    collection_image_data(collection='images-V', image_access_restrictions="CC-BY-NC-ND"),
    collection_image_data(collection='images-M', image_access_restrictions="CC-BY-NC-ND"),
])
def test_is_digital_library(collection, image_data):
    """These examples all end up in the Digital Library."""
    assert sort_image(collection, image_data) == Decision.catalogue_api
