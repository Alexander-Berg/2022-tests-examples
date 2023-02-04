# from ..grocery.targets.shelve_target import ShelveTarget
#
#
# class TestShelve:
#
#     def setup(self):
#         self.target = ShelveTarget("./_test/dwh-200/pub", 'ololo')
#
#     def test_no_exists(self):
#         self.target.write(1)
#         self.target.delete()
#         assert not self.target.exists()
#
#     def test_exists(self):
#         self.target.write(1)
#         assert self.target.exists()
#
#     def teardown(self):
#         try:
#             self.target.delete()
#         except KeyError:
#             pass
