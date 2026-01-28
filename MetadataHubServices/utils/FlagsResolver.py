class FlagsResolver:
    """
    Resuelve flags del request y determina comportamiento de busqueda.
    """

    @staticmethod
    def should_use_filter(flags):
        """
        Determina si se debe usar filtro 'songs' en la busqueda.
        Returns: True si NO tiene flag 'raw', False si tiene 'raw'
        """
        if not flags:
            return True

        flags_lower = [f.lower() for f in flags]
        return 'raw' not in flags_lower

    @staticmethod
    def has_flag(flags, flag_name):
        """
        Verifica si existe un flag especifico.
        """
        if not flags:
            return False

        flags_lower = [f.lower() for f in flags]
        return flag_name.lower() in flags_lower