from pathlib import Path

workflow = Path('.github/workflows/android-build.yml')
helper_workflow = Path('.github/workflows/birdie-gradient-patch.yml')
patch_script = Path('scripts/birdie_gradient_patch.py')
protected = {str(workflow), str(helper_workflow), str(patch_script)}

_original_write_text = Path.write_text
_original_unlink = Path.unlink


def guarded_write_text(self, data, *args, **kwargs):
    if str(self) == str(workflow):
        return len(data)
    return _original_write_text(self, data, *args, **kwargs)


def guarded_unlink(self, *args, **kwargs):
    if str(self) in protected:
        return None
    return _original_unlink(self, *args, **kwargs)


Path.write_text = guarded_write_text
Path.unlink = guarded_unlink
exec(compile(patch_script.read_text(), str(patch_script), 'exec'))
